# Compiled diagnostic rule catalog

These identifiers preserve existing interpreter meanings in semantic output 0.1.
All current severities are error. Only input-unavailable and no-source-files have
phase input; all others have phase source. Messages are explanatory, not stable
API strings. Future additions require contract review; changed meanings need a
new rule identifier. Source contracts remain the authority for validity.

| Rule ID | Meaning |
| --- | --- |
| byte-order-mark | Source starts with a forbidden UTF-8 BOM. |
| control-character | Source contains a forbidden physical control character. |
| dangling-reference | A decomposes target is absent from the selected set. |
| duplicate-id | An ID has multiple definitions in the selected set. |
| final-line-ending | Source lacks its required final line ending. |
| input-unavailable | A selected path cannot be discovered or read. |
| invalid-id | A requirement ID does not follow the selected ID rules. |
| invalid-utf8 | Source bytes cannot be decoded as UTF-8. |
| line-ending | A carriage return is not followed by LF. |
| no-source-files | Selection contains no eligible source files. |
| nul-byte | Source contains NUL. |
| tab | Source contains a forbidden physical tab. |
| yaml-character | A decoded YAML value contains forbidden characters. |
| yaml-duplicate-key | A YAML mapping repeats a key. |
| yaml-duplicate-target | A YAML relationship sequence repeats a target. |
| yaml-limit | The YAML byte, depth, record or diagnostic resource limit is exceeded. |
| yaml-profile | The input uses a disallowed YAML document/directive/tag/anchor/alias feature. |
| yaml-scalar-boundary | A decoded YAML scalar has invalid boundary whitespace. |
| yaml-scalar-style | A YAML scalar value uses a disallowed plain style. |
| yaml-schema | A YAML key, collection, scalar type or required structural element violates the requirements profile. |
| yaml-syntax | YAML parsing fails. |
| yaml-version | The root format identifier is unsupported. |

## Project attribute source rules

YAML 0.4 adds attribute-schema-unavailable (input), attribute-schema-invalid,
attribute-schema-duplicate, attribute-schema-required, attribute-schema-mismatch,
attribute-unknown, attribute-value and attribute-required (source). These are errors;
[0020](0020-project-attributes-yaml-0.4.md) defines their locations and suppression.

Before publishing YAML 0.4 output, detected requirement or declaration edits produce
`input-changed` or `attribute-schema-changed`, respectively (input phase, exit 2,
no requirement records). [Output 0.2](0021-requirement-semantic-output-0.2.md) defines
these snapshot checks. Formatter declaration changes use `attribute-schema-changed`
within its existing write-failure report.
