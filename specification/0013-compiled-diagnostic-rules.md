# Compiled diagnostic rule catalog

These identifiers preserve existing interpreter meanings in semantic output 0.1.
All current severities are error. Only input-unavailable and no-source-files have
phase input; all others have phase source. Messages are explanatory, not stable
API strings. Future additions require contract review; changed meanings need a
new rule identifier. Source contracts remain the authority for validity.

| Rule ID | Meaning |
| --- | --- |
| body-indentation | Custom body indentation is invalid. |
| byte-order-mark | Source starts with a forbidden UTF-8 BOM. |
| content-outside-record | Nonblank custom source occurs outside a requirement. |
| control-character | Source contains a forbidden physical control character. |
| dangling-reference | A decomposes target is absent from the selected set. |
| duplicate-field | A custom field is repeated illegally. |
| duplicate-id | An ID has multiple definitions in the selected set. |
| duplicate-relationship | A custom decomposes target repeats. |
| empty-body | A custom body contains no content. |
| empty-math | A custom math block contains no payload. |
| empty-or-padded-scalar | A custom scalar is empty or has boundary whitespace. |
| empty-source-file | Custom file contains no requirement. |
| field-form | A custom field does not use its required scalar/body form. |
| final-line-ending | Source lacks its required final line ending. |
| input-unavailable | A selected path cannot be discovered or read. |
| invalid-id | A requirement ID does not follow the selected ID rules. |
| invalid-reference-id | A custom decomposes target is not a valid ID. |
| invalid-utf8 | Source bytes cannot be decoded as UTF-8. |
| line-ending | A carriage return is not followed by LF. |
| malformed-record | Custom syntax is not a recognized field or terminator. |
| math-indentation | Custom math payload indentation is invalid. |
| missing-field | A required custom field is missing. |
| missing-record-end | Custom record lacks its terminator. |
| nested-record | A custom opener occurs before the prior record ends. |
| no-source-files | Selection contains no eligible source files. |
| nul-byte | Source contains NUL. |
| out-of-order-field | A custom field occurs in the wrong order. |
| recovery-limit | Custom record recovery reached its 100-diagnostic per-file bound. |
| record-separation | Custom records lack required blank/comment separation. |
| tab | Source contains a forbidden physical tab. |
| unexpected-math-end | A math terminator occurs without an opener. |
| unknown-field | A custom field name is not permitted. |
| unmatched-record-end | Custom record terminator has no opener. |
| unterminated-math | A custom math block has no terminator. |
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
