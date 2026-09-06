# Project-defined requirement attributes

The opt-in YAML 0.4 profile supports descriptive text and enum attributes defined
in an explicitly selected JSON project declaration. The declaration and requirement
source are authoritative; generated artifacts are derived. Requiredness is checked
without default values. Classifications do not establish assessment authority.

Use the [declaration](../examples/attributes/requirement-attributes.json),
[requirements](../examples/attributes/system.mreq.yaml), and
[normative contract](../specification/0020-project-attributes-yaml-0.4.md).

```sh
build/maintained/mundanereq-validate --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json examples/attributes/system.mreq.yaml
build/maintained/mundanereq-format --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json --check examples/attributes/system.mreq.yaml
build/maintained/mundanereq-trace --source=yaml-0.4 --attribute-schema examples/attributes/requirement-attributes.json parents SYS-001 examples/attributes/system.mreq.yaml
```

Formatting validates all values, preserves authored order/comments/quotes/indentation
and changes only CRLF to LF. It never formats the JSON declaration. Each replacement
rechecks declaration and source snapshots. A detected intervening edit returns 2,
preserves that external edit and lists completed/remaining files. A race remains
between the final check and filesystem rename; this is not a multi-file transaction.
Trace validates attributes but derives edges exclusively from explicit decomposes.
Invalid schema/value input yields no writes or usable trace output.

The YAML 0.3 profile remains supported. Adoption requires a deliberate 0.4
header change, matching attributeSchema name and explicit --attribute-schema option.
Omit both schema name/option and attributes for schema-free 0.4. Existing source need
not migrate. Unsupported selectors/formats fail instead of silently dropping values.

Compilation is available with an explicit source root:

```sh
build/maintained/mundanereq-compile --source=yaml-0.4 --root . --attribute-schema examples/attributes/requirement-attributes.json examples/attributes/system.mreq.yaml > requirements.json
```

[Output 0.2](../specification/0021-requirement-semantic-output-0.2.md) preserves
values, typed declarations and separate source provenance. Schema-free YAML 0.4
also emits 0.2, with null schema and empty attributes. Existing profiles emit 0.1.
The compiler rechecks selected snapshots before publication; a detected source or
schema edit returns 2 and no usable records. Old consumers reject the new format.

The maintained linker, verifier and work-item analyzer accept requirement output 0.2
without loading its source files. They validate declarations, requiredness, values,
provenance and locations before publishing edges. Verification selects linked and
analysis output 0.2 whenever any imported requirements use output 0.2. All-old inputs
keep their original output. [The comparison contract](../specification/0022-attribute-linking-and-analysis-0.2.md)
compares present values and the whole canonical declaration: even description-only
or unused enum-vocabulary edits request review. Comments, ordering and paths alone
do not. Schema names are scoped, not merged. Work-item typed links retain their
existing meaning and do not infer edges from attributes.

The [experimental verification report](../experiments/0029-verification-report/README.md)
accepts analysis 0.1 and 0.2. For 0.2 it independently validates serialized attributes
and recomputes differences before display. Present values, optional absence, types,
requiredness, descriptions and declaration/value/assertion source locations are
visible. Changed bindings include both baseline and current values and full schema
definitions. Literal HTML-looking text is escaped. Generated views remain disposable.

| Consumer | YAML 0.4 / attributes | Scope |
| --- | --- | --- |
| Validator / SARIF | Full selected-profile validation | Explicit JSON declaration; structural schemas alone do not validate project enum/requiredness rules |
| Formatter | Lossless except CRLF normalization | Preserves comments and authored order; does not format declarations |
| Compiler / linker / verifier | Lossless semantic propagation | Explicit output 0.2; canonical declarations and retained source provenance |
| Work-item analyzer | Strict requirement imports and typed links | No implicit edges or attribute assessment authority |
| Experimental review report | Full typed semantic display | Source-independent rendering; no original lexical/layout preservation or editing |
| Editor host, hover, enum/name completion | Unimplemented | Future integration must select a project declaration and use semantic validation |

ReqIF interchange is not implemented. Attribute adoption is an explicit source/configuration change.

For a complete executable workflow and the 57-requirement adoption example, see
[the example guide](../examples/attributes/README.md). An attribute declaration is
not itself a JSON Schema document. The checked-in structural schemas describe the
language/declaration shape; project-specific enum membership, requiredness, names,
physical limits and explicit selection remain compiler rules. Editors may use those
structural schemas where supported, but this repository provides no editor host,
project-schema completion, hover or equivalent semantic-validation integration.
