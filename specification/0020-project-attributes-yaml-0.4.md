# Project-defined requirement attributes: YAML 0.4

Normative experimental source and declaration contract. This extends the built-in
model and YAML presentation rules in [0010](0010-requirements-yaml-0.3.md) and command
safety in [0011](0011-tool-safety-and-yaml-commands.md). Old profiles are unchanged.
The [YAML authoring policy](0027-yaml-authoring-policy.md) controls presentation. This document controls attribute semantics. Types describe author-supplied metadata, never assessments,
allocation authority, test execution, requirement satisfaction or safety approval.

## Declaration

Explicitly select one checked-in YAML file using `--attribute-schema PATH` with
`--source=yaml-0.4`. This YAML mapping is a project declaration, not JSON Schema.
Its [structural schema](schema/attribute-declaration-0.1.json) describes decoded
shape; duplicate keys, byte limits/encoding and semantic lexical rules also apply.
See the [complete example](../examples/attributes/requirement-attributes.yaml).

Root keys are exactly format (`mundanereq-attributes-yaml-0.1`), name and attributes.
Name and attribute keys match `[a-z][a-z0-9]*(?:-[a-z0-9]+)*`, maximum 64 ASCII
characters. Reserve built-ins id/title/allocation/statement/rationale/source/decomposes,
format/requirements/attributes/attribute-schema and names beginning mreq- or mundane-.
No overriding or aliases. The name binds a source document to an explicitly selected
vocabulary; it is not a globally unique identity or revision pin.

There are 1–128 attributes. Every declaration has type, required and description.
Type is text or enum; required is a YAML boolean; description is nonempty, unpadded,
single-line Unicode text using the existing title character rules. Enum declarations
also require 1–256 unique values with the same text rules. Membership is exact and
case-sensitive, without normalization; order has no semantic meaning. Text forbids
values. Unknown fields, defaults, imports, inheritance, patterns and references fail.
Only explicitly present values exist; no nulls, defaults, lists or repeated values.

Require UTF-8 without BOM, final LF or CRLF, no bare CR, invalid Unicode, unsupported YAML features. Maximum 1 MiB and 16 collection levels; duplicate keys
at every depth fail before object construction. Limits fail rather than truncate.
The declaration snapshot retains token locations, exact bytes and file identity.

## Requirement source and selection

Use format `mundanereq-yaml-0.4` with the existing requirements sequence and built-ins.
Optional root attributeSchema is a quoted project schema name. Optional record
attributes is a nonempty mapping of declared names to quoted or block strings;
omit it for no values. Attribute keys follow the declaration naming rules; duplicates
fail even if their values agree. Values obey the same nonempty/unpadded/single-line
text rules as descriptions. Block strings are allowed only when their decoded
value meets those rules. Attribute text never creates implicit reference edges.
The [source schema](schema/requirements-yaml-0.4.json) covers structure independently
of the selected project's vocabulary; compilation validates vocabulary/requiredness.

- With an explicit declaration, every selected document must name that declaration,
  including documents with no values when all attributes are optional.
- A document name without the option is an error; there is no discovery or fallback.
- With neither name nor option, the built-in model remains valid. Attributes are
  prohibited in such schema-free source. Old profiles reject both new fields/option.
- An isolated requirement file may be checked with an explicit declaration and its
  ordinary reference context. Files under different declarations compile separately.

The leading source selector is explicit. Accept the schema option once before `--`;
resolve it against the working directory. Reject duplicate options and use with an
unsupported command/profile. There is no network/search path/environment selection.
The declaration is a regular non-symlink file and is not a requirement discovery
candidate or formatter target. Explicit compiler/SARIF roots must contain it, also
after resolution; outside-root inputs are invocation failures. Without those roots,
resolve only the explicit file. No implicit project root is introduced.

## Validation and diagnostics

Validation reads one bounded schema snapshot before interpreting values. Invalid
or unavailable declarations suppress attribute cascades and make interpretation
incomplete. Valid declarations permit ordinary bounded record recovery; valid
neighbors remain available internally but never authorize complete output on failure.
Existing source physical/Unicode, key, scalar, YAML feature and target checks apply.

| Failure | Rule and location | Validator exit |
| --- | --- | --- |
| Unsupported/repeated option or outside root | Invocation stderr, no machine document | 2 |
| Missing/unreadable/nonregular declaration | attribute-schema-unavailable, path without fabricated SARIF region | 2 |
| Invalid JSON, declaration or bound | attribute-schema-invalid, offending token or containing object | 1 |
| Duplicate JSON key | attribute-schema-duplicate, repeated key | 1 |
| Missing explicit schema | attribute-schema-required, document name or attachment | 1 |
| Missing/different document name | attribute-schema-mismatch, name or document start | 1 |
| Unknown attribute | attribute-unknown, key | 1 |
| Invalid type/string/enum member | attribute-value, value | 1 |
| Required attribute absent | attribute-required, requirement start | 1 |
| Duplicate YAML attribute | yaml-duplicate-key, repeated key | 1 |

SARIF uses the same rules, severity error, code-point source positions and incomplete
state. Operational failures have no invented region; failed output overrides success
with exit 2. Human-authored requirement IDs remain unchanged.

## Propagation obligations

Each command enables YAML 0.4 only with its own implementation. Until then it rejects
the selector. Formatter/trace must validate the full selected schema and source;
formatting may normalize CRLF only and must recheck the schema before each replacement.
Detected schema edits stop remaining writes with attribute-schema-changed and existing
completed/remaining-file reporting. Preserve the documented final-check/rename race.
No declaration formatting is allowed. Trace adds no attribute edges.

Compiled output, linking, review comparison and report contracts are versioned in
their owning addenda. ReqIF interchange remains unsupported for
new attribute-bearing input; no flattening is allowed. Structural editor assistance
alone cannot assert semantic validity. Other artifact formats are independent choices.

Formatter and trace support is implemented by TC-1304. Their YAML 0.4 command
contracts are formatter-cli-0.1+safety-1+attributes-1 and
trace-cli-0.1+safety-1+attributes-1. Old-profile contract identifiers remain unchanged.

## Worked adoption and downstream contracts

The [example guide](../examples/attributes/README.md) demonstrates explicit header,
project-name and command-option changes without rewriting existing projects. See
[output 0.2](0021-requirement-semantic-output-0.2.md) for derived declarations and
[analysis 0.2](0022-attribute-linking-and-analysis-0.2.md) for conservative review
comparison. Current implementation/capability limits are in the
[command guide](../distribution/attributes.md); source text remains authoritative.
