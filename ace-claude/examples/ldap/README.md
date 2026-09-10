# LDAP connector — ApplicationConnector Request node (runtime-proven)

Copy-ready flows for `ComIbmApplicationConnectorRequest_ldap`. Workspace proof:
`demo-apps/LDAP_DEMO_APP`, driven against OpenLDAP and **validated in the ACE
Toolkit (BAR built cleanly)**.

Same node family as Amazon S3, so the rules in `examples/s3/` apply — including
both `<filter>` forms. Read that README too.

| File | Action | Class |
|---|---|---|
| `LDAP_SEARCH_MF` | `RETRIEVEALL` | filter (`<queryProperties limit=…/>`) |
| `LDAP_CREATE_MF` | `CREATE` | body |
| `LDAP_UPDATE_MF` | `UPDATEALL` | body **and** filter |
| `LDAP_DELETE_MF` | `DELETEALL` | filter (where clause on `dn`) |

## businessObject is the LDAP object class, not `entry`

LDAP uses **dynamic objects** (`descriptors/ldap.yaml` → `dynamicObjects`) and
ships no `objects.json`, so the business object is the class itself:

```xml
businessObject="inetOrgPerson"
```

Using `entry` — the name of the model file — fails with
`The class definition for the object or its parent object is either missing or invalid`.
`entry.json` is only a template. Supported classes, per the connector's own error
string: organizationalUnit, user, InetOrgPerson, group, contact, computer.

Because the object is dynamic, its **own schema attributes are first-class body
properties**, and the generic template fields are not accepted. A where clause on
`searchCriteria` or `scope` is rejected with
`is not allowed to have the additional property "searchCriteria"`, and
`ldapObjectClass` is rejected on create (implied by `businessObject`) even though
the template lists it as mandatory.

## The connector builds the DN itself — this is the part that bites

`@ibm-app-connect/ldap-api-utils/lib/util/requestUtil.js` → `formCreateReqObject`:

```js
s.dn  = "cn=" + t.cn  [+ ",l="] [+ ",st="] [+ ",o="] [+ ",c="] [+ ",street="] [+ ",uid=" + t.uid]
s.dn += "," + (t.ou || r)
delete s.entry.ou
```

Consequences:

- **`ou` carries the FULL container DN** (`ou=people,dc=example,dc=com`) and is
  deleted from the attributes, so it never reaches the directory. A bare
  `ou="people"` builds `cn=X,people` → `Invalid distinguished name`.
- **`baseDN` is NOT used for these classes** — put it in the body and it is sent
  as an LDAP attribute: `ldapErrCode 17 baseDN: attribute type undefined`.
  (`baseDN` *is* correct as a `<connectorProperty/>` for `RETRIEVEALL`.)
- **`uid`, `l`, `st`, `o`, `c`, `street` become DN COMPONENTS, not attributes.**
  Sending `uid` builds `cn=Carol,uid=carol,ou=people,…`, whose parent does not
  exist → `ldapErrCode 32 No Such Object`. The error quotes the deepest *matched*
  ancestor, which makes it look as though an existing DN was missing.
- **`cn` is the RDN** for inetOrgPerson (`lib/constants.json` →
  `createMandatoryFields`).

## Multi-valued attributes must be JSON arrays

Even for a single value — otherwise `is not of a type(s) array`. `uid` and `cn`
are scalars; `sn` and `mail` are arrays:

```sql
CREATE FIELD OutputRoot.JSON.Data.sn IDENTITY (JSON.Array)sn;
SET OutputRoot.JSON.Data.sn.Item[1] = InputRoot.JSON.Data.sn;
```

## Update and delete: where clause on `dn`

Both key on `dn` (`constants.json` → `updateDeleteMandatoryFilter`). No
`<connectorProperty/>` parent row is needed — a full `dn` suffices:

```xml
<filter>
  <filterElementObject type="where">
    <filterElementArray type="and">
      <filterProperty propertyName="dn" displayName="Distinguished name"
                      propertyValue="[[$Environment/ldapDn]]" compareAction=""/>
    </filterElementArray>
  </filterElementObject>
</filter>
```

with `SET Environment.ldapDn = InputRoot.JSON.Data.dn;` upstream. `UPDATEALL`
additionally takes the new values in the body. Despite the name, `DELETEALL`
deletes exactly the one entry the filter selects.

## The Input node does NOT work — do not use it

`ComIbmApplicationConnectorInput_ldap` packages, deploys and reports
`BIP2269I ... started successfully`, then fails in a **retry loop** roughly once a
second, emitting nothing:

```
BIP9937E: TypeError: i.getModel(...).subscribe is not a function
BIP5073E: Failed to establish connection to 'LDAP'
BIP9953E: An error occurred while trying to receive an event from the first application.
```

ACE ships event/input support as **separate packages**,
`loopback-connector-<type>**event**` (17 are installed). There is no
`loopback-connector-ldapevent`, and the base connector implements no
`subscribe`/`unsubscribe`.

**Before authoring any `ComIbmApplicationConnectorInput_*` node, check that
`loopback-connector-<type>event` exists in
`<ACE>/server/nodejs_all/node_modules/@ibm-app-connect/`.** The "Has Input node"
column in the SKILL.md connector table is a design-time fact — the msgnode exists
and the Toolkit offers the node — and says nothing about runtime support.

## Setup

```bash
docker run -d --name ldap -p 1389:389 \
  -e LDAP_ORGANISATION="Demo" -e LDAP_DOMAIN="example.com" \
  -e LDAP_ADMIN_PASSWORD="<password>" osixia/openldap:1.5.0

mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type ldap --credential-name LdapCred \
  --username "cn=admin,dc=example,dc=com" --password "<password>"
```

Policy attributes come from `common/schemas/Connectors/PolicyConnectors.xsd`
(className="ldap"): `credentialName`, `applicationVersion` (`v1`),
`applicationType` (`online`), `authenticationMethod` (**`BASIC` is the only legal
value**), `endpointUrl`. Do not put a base DN in `endpointUrl` — keep it
`ldap://host:port`.

The image entrypoint is a wrapper, so use `docker exec ldap ldapsearch ...` or
`docker run --rm --network host --entrypoint ldapsearch osixia/openldap:1.5.0 ...`
to verify independently of the flows.
