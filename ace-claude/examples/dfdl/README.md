# DFDL examples (verified working, ACE 13.0.2.2)

Read `ace-claude/DFDL.md` first — it explains the schema anatomy, node
configuration, and error signatures. This directory holds the verified
artifacts.

`DFDL_APP_TEMPLATE/` is a copy of workspace project `DFDL_DEMO_APP`, whose
two flows packaged, deployed, and passed HTTP smoke tests.
Copy the whole directory, rename the project in `.project` (**never leave a
name that collides with a real project** — ibmint scans the whole
`--input-path`; duplicates → BIP8081E), and mutate.

## Contents

| File | What it proves |
|---|---|
| `DFDL_APP_TEMPLATE/Company.xsd` | DFDL schema: pre-canned format import, `ibmDfdlExtn:docRoot`, initiators/terminators, `%CR;%LF;` / `%#124;` entities, implicit occurs, nillable `%ES;`, `textNumberPattern` |
| `DFDL_APP_TEMPLATE/IBMdefined/RecordSeparatedFieldFormat.xsd` | imported base format, relative `schemaLocation` preserved inside the app |
| `DFDL_APP_TEMPLATE/DFDL_PARSE_MF.*` | **parse direction**: HTTPInput `messageDomainProperty="DFDL"` `messageTypeProperty="{}:Company"` → ESQL walks `InputRoot.DFDL`, emits a real JSON array (`(JSON.Array)` + `Item[i]`) |
| `DFDL_APP_TEMPLATE/DFDL_WRITE_MF.*` | **write direction**: JSON in → build `OutputRoot.DFDL.Company...` → WSReply serializes; no `Properties.MessageType` needed |
| `company.txt` | test payload (from `<ACE_INSTALL>/server/sample/dfdl/`) — CRLF line endings are load-bearing; an LF-only copy fails with CTDP3042E |

## Verified smoke tests

```bash
# parse: tagged-delimited text -> JSON (5 employees, nil dept -> null)
curl -s http://localhost:7800/dfdl/parse --data-binary @company.txt

# write: JSON -> byte-exact tagged-delimited text (CRLF included)
curl -s http://localhost:7800/dfdl/write -H "Content-Type: application/json" \
  -d '{"company":{"name":"My Company","employees":[{"empNo":111111,"dept":500,
       "name":"Alice Wong","street":"8200 Warden Ave","city":"Markham",
       "zip":"L3G 1H7","tel":"905-347-5649","salary":135599.95}]}}'
```

Round trip confirmed: parse output fed back through write reproduces the
input format; nillable empty `dept=` survives as JSON `null`.
