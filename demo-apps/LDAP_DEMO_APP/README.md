# LDAP_DEMO_APP — LDAP connector CRUD

Four HTTP-fronted flows over `ComIbmApplicationConnectorRequest_ldap`.
**All runtime-verified** against OpenLDAP, and **Toolkit-validated** (BAR built
cleanly in the ACE Toolkit). Full reference: `ace-claude/examples/ldap/`.

| Flow | Endpoint | Action | Request shape |
|---|---|---|---|
| `LDAP_SEARCH_MF` | `POST /ldap/search` | `RETRIEVEALL` | filter — `<queryProperties limit=…/>` + `baseDN` connectorProperty |
| `LDAP_CREATE_MF` | `POST /ldap/create` | `CREATE` | body — `ou` (full container DN) + `cn` + array attrs |
| `LDAP_UPDATE_MF` | `POST /ldap/update` | `UPDATEALL` | body **and** where-clause filter on `dn` |
| `LDAP_DELETE_MF` | `POST /ldap/delete` | `DELETEALL` | filter — where clause on `dn` |

`businessObject="inetOrgPerson"` — the **LDAP object class**, not `entry`.

## Setup

```bash
docker run -d --name ldap -p 1389:389 \
  -e LDAP_ORGANISATION="Demo" -e LDAP_DOMAIN="example.com" \
  -e LDAP_ADMIN_PASSWORD="<password>" osixia/openldap:1.5.0

# seed a container for the entries
cat > /tmp/seed.ldif <<'LDIF'
dn: ou=people,dc=example,dc=com
objectClass: organizationalUnit
ou: people
LDIF
docker cp /tmp/seed.ldif ldap:/tmp/ && docker exec ldap \
  ldapadd -x -H ldap://localhost -D "cn=admin,dc=example,dc=com" -w "<password>" -f /tmp/seed.ldif

mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type ldap --credential-name LdapCred \
  --username "cn=admin,dc=example,dc=com" --password "<password>"

ibmint package --input-path <workspace> --output-bar-file bars/LDAP_DEMO_APP.bar \
  --project LDAP_DEMO_APP --project LDAP_DEMO_POLICIES
ibmint deploy --input-bar-file bars/LDAP_DEMO_APP.bar --output-work-directory <work-dir>
```

Point `LDAP_DEMO_POLICIES/LdapLocal.policyxml` at your directory, and change the
container DN (`ou=people,dc=example,dc=com`) in the ESQL if yours differs.

## Driving it

```bash
curl -X POST http://localhost:7800/ldap/create -H 'Content-Type: application/json' \
  -d '{"cn":"Dave Test","sn":"Test","mail":"dave@example.com"}'

curl -X POST http://localhost:7800/ldap/search -H 'Content-Type: application/json' -d '{}'

curl -X POST http://localhost:7800/ldap/update -H 'Content-Type: application/json' \
  -d '{"dn":"cn=Dave Test,ou=people,dc=example,dc=com","mail":"dave.new@example.com"}'

curl -X POST http://localhost:7800/ldap/delete -H 'Content-Type: application/json' \
  -d '{"dn":"cn=Dave Test,ou=people,dc=example,dc=com"}'
```

Verify against the directory rather than trusting the flow reply:

```bash
docker exec ldap ldapsearch -x -H ldap://localhost -b "dc=example,dc=com" \
  -D "cn=admin,dc=example,dc=com" -w "<password>" "(objectClass=inetOrgPerson)" dn
```

## Two things that will catch you out

**Do not send `uid` on create.** The connector builds the DN itself and appends
`uid`, `l`, `st`, `o`, `c`, `street` as **DN components**. `uid` produces
`cn=X,uid=y,ou=people,…`, whose parent does not exist, and the resulting
`No Such Object` error quotes a DN that does exist — see `examples/ldap/README.md`.

**Multi-valued attributes must be JSON arrays**, even with one value
(`sn`, `mail`), while `cn` and `uid` are scalars.

## Not included: the Input node

`ComIbmApplicationConnectorInput_ldap` deploys and reports success, then fails in
a retry loop — ACE has no `loopback-connector-ldapevent` package. Details and the
general rule are in `ace-claude/examples/ldap/README.md`.
