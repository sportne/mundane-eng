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

Compilation is available with an explicit source root:

```sh
build/maintained/mundanereq-compile --source=yaml-0.4 --root . --attribute-schema examples/attributes/requirement-attributes.json examples/attributes > requirements.json
```

[Output 0.2](../specification/0021-requirement-semantic-output-0.2.md) preserves
values, typed declarations and separate source provenance. Schema-free YAML 0.4
also emits 0.2, with null schema and empty attributes. Existing profiles emit 0.1.
The compiler rechecks selected snapshots before publication; a detected source or
schema edit returns 2 and no usable records. Old consumers reject the new format.
