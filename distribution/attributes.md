# Project-defined requirement attributes

The opt-in YAML 0.4 profile supports descriptive text and enum attributes defined
in an explicitly selected JSON project declaration. The declaration and requirement
source are authoritative; generated artifacts are derived. Requiredness is checked
without default values. Classifications do not establish assessment authority.

Use the [declaration](../examples/attributes/requirement-attributes.json),
[requirements](../examples/attributes/system.mreq.yaml), and
[normative contract](../specification/0020-project-attributes-yaml-0.4.md).

```sh
build/maintained/mundanereq-validate --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json examples/attributes
build/maintained/mundanereq-format --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json --check examples/attributes
build/maintained/mundanereq-trace --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json parents SYS-001 examples/attributes
```

Formatting validates all values, preserves authored order/comments/quotes/indentation
and changes only CRLF to LF. It never formats the JSON declaration. Each replacement
rechecks declaration and source snapshots. A detected intervening edit returns 2,
preserves that external edit and lists completed/remaining files. A race remains
between the final check and filesystem rename; this is not a multi-file transaction.
Trace validates attributes but derives edges exclusively from explicit decomposes.
Invalid schema/value input yields no writes or usable trace output.

Old profiles and commands remain unchanged. Adoption requires a deliberate 0.4
header change, matching attributeSchema name and explicit --attribute-schema option.
Omit both schema name/option and attributes for schema-free 0.4. Existing source need
not migrate. Compilation and downstream capabilities are enabled by their owning
cards; unsupported selectors/formats fail instead of silently dropping values.
