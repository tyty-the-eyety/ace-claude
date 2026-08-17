# DFDL domain (verified working, ACE 13.0.2.2)

DFDL (Data Format Description Language) models text/binary wire formats —
CSV, fixed-length records, tagged delimited, COBOL copybook data — as a plain
XSD annotated with `dfdl:` properties. The runtime parses raw bytes into a
logical tree at `InputRoot.DFDL` and serializes `OutputRoot.DFDL` back to
bytes. Both directions verified 2026-07-06 (workspace project
`DFDL_DEMO_APP`, HTTP round trip against the shipped `company.txt` sample).

**Do not author a DFDL schema from scratch.** Copy
`examples/dfdl/DFDL_APP_TEMPLATE/` (whole app: schema + parse flow + write
flow) and mutate it — see `examples/dfdl/README.md` for what was verified.

## Where the shipped references live

- `<ACE_INSTALL>/server/sample/dfdl/` — `company.xsd` (worked example using
  initiators/terminators/separators/nillable/escape schemes), `company.txt`
  (matching input data, CRLF line endings), `company.xml` (expected logical
  result), `IBMdefined/RecordSeparatedFieldFormat.xsd`
- `<ACE_INSTALL>/tools/plugins/com.ibm.dfdl.precanned.formats_*.jar` —
  `formats/ibm/*.xsd`: `CommaSeparatedFormat`, `RecordSeparatedFieldFormat`,
  `RecordFixLengthFieldFormat`, `GeneralPurposeFormat`,
  `CDataDefinitionFormat`, `CobolDataDefinitionFormat`,
  `HLASMDataDefinitionFormat`; plus `templates/*.xsd` starter schemas
  (`record_csv_template.xsd` etc. — extract with `unzip -p`)

## Schema anatomy (from the verified Company.xsd)

1. **Import a pre-canned base format** and reference it in a schema-level
   default format block:
   ```xml
   <xsd:import namespace="http://www.ibm.com/dfdl/RecordSeparatedFieldFormat"
               schemaLocation="IBMdefined/RecordSeparatedFieldFormat.xsd"/>
   <xsd:annotation>
     <xsd:appinfo source="http://www.ogf.org/dfdl/">
       <dfdl:format ref="recSepFieldsFmt:RecordSeparatedFieldsFormat"
                    encoding="{$dfdl:encoding}"
                    escapeSchemeRef="recSepFieldsFmt:RecordEscapeScheme"
                    occursCountKind="fixed"
                    nilValue="%ES;" nilKind="literalValue" useNilForDefault="no"/>
     </xsd:appinfo>
   </xsd:annotation>
   ```
   Keep the imported XSD in the app project and preserve the relative
   `schemaLocation` path (e.g. an `IBMdefined/` subfolder). `ibmint package`
   picks both files up automatically — no message-set step exists in ACE.
2. **Mark the document root**: `ibmDfdlExtn:docRoot="true"` on the root
   `xsd:element` (namespace `xmlns:ibmDfdlExtn="http://www.ibm.com/dfdl/extensions"`).
3. **Delimiters** are per-element/per-sequence attributes:
   `dfdl:initiator="Employee("`, `dfdl:terminator=")%CR;%LF;"`,
   `dfdl:separator="%#124;"` (that is `|` — DFDL entities: `%CR;%LF;` for
   CRLF, `%#124;` for a literal by codepoint, `%ES;` for empty string).
4. **Repeating element**: `maxOccurs="unbounded"` +
   `dfdl:occursCountKind="implicit"` — the parser keeps consuming while the
   initiator matches.
5. **Optional trailing fields**: `dfdl:separatorPolicy="suppressedAtEndLax"`
   on the sequence.
6. **Nillable field**: `nillable="true"` on the element; with
   `nilValue="%ES;"` an empty `dept=` parses to a logical null (and arrives
   in ESQL as SQL NULL).
7. **Numbers**: `dfdl:textNumberPattern="#0"` (integer) / `"#0.###"`
   (decimal). NOTE `#0.###` drops trailing zeros on WRITE (`50000.00` →
   `50000`); use `"#0.00"` if the wire format requires fixed decimals.

## Message flow node configuration

On the input node (HTTPInput, MQInput, FileInput — same three attributes):

```xml
<nodes xmi:type="ComIbmWSInput.msgnode:FCMComposite_1" ...
    messageDomainProperty="DFDL"
    messageTypeProperty="{}:Company">
```

- `messageTypeProperty` (Toolkit label "Message") is the root element as
  `{namespace}:element` — `{}:Company` for a no-namespace schema.
- `messageSetProperty` (Toolkit label "Message model") stays **unset** for
  application-deployed schemas.
- Output side needs nothing: build `OutputRoot.DFDL.<Root>...` and any
  output/reply node serializes it. The serializer resolves the schema by
  matching the tree's root element name against the deployed schemas —
  setting `OutputRoot.Properties.MessageType` is NOT required (verified by
  removing it: byte-identical output).

## ESQL access

```esql
-- read (parse direction)
DECLARE dfdlCompany REFERENCE TO InputRoot.DFDL.Company;
SET ... = dfdlCompany.CompanyName;            -- plain field
SET ... = dfdlCompany.Employee[1].Address.City; -- nested/repeating
-- nillable wire field arrives as NULL — becomes null in JSON output

-- write (serialize direction)
SET OutputRoot.DFDL.Company.CompanyName = ...;
SET OutputRoot.DFDL.Company.Employee[i].EmpNo = CAST(... AS INTEGER);
```

Numeric fields typed `xsd:integer`/`xsd:decimal` in the schema surface as
typed values (no string casts needed on read; CAST on write when the source
is a JSON string).

**Parsing is lazy (on-demand):** with a malformed payload the input node
succeeds and the failure surfaces at the first ESQL navigation into
`InputRoot.DFDL` — as `BIP2230E` at the *Compute node*, wrapping `BIP5803E` /
`BIP5807E`. To fail at the input node instead, set the node attribute
`validateTiming="immediate"` (Toolkit label "Parse timing"; values
`deferred`/`immediate`/`complete`, default deferred = on demand).

## Reading DFDL parse errors

`BIP5807E` carries the DFDL parser's own `CTDP` message, which names the
element path and byte offset — read it before touching the schema:

- `CTDP3042E: Terminator '%CR;%LF;' not found at offset '659' for element
  '/Company[1]/CompanyName[1]'` — data had LF-only line endings (delimiters
  must match byte-for-byte)
- `CTDP3058E: Separator ',' not found at offset '100' for sequence within
  element '/Company[1]/Employee[1]/Address[1]'` — truncated record

## Gotcha: JSON arrays in ESQL output

`OutputRoot.JSON.Data.x.items[i].field` alone does NOT make a JSON array —
it produces repeated `"items"` object keys (invalid JSON). Declare the array
field first, then index anonymous `Item` children:

```esql
CREATE FIELD OutputRoot.JSON.Data.company.employees IDENTITY (JSON.Array)employees;
SET OutputRoot.JSON.Data.company.employees.Item[i].empNo = ...;
```
