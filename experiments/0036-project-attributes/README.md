# Experiment 0036: Project attribute workflow evidence

This bounded corpus owns integration evidence for TC-1303–1308. Its checked-in
[examples](../../examples/attributes/README.md), [source contract](../../specification/0020-project-attributes-yaml-0.4.md)
and [compiled contracts](../../specification/0022-attribute-linking-and-analysis-0.2.md)
cover descriptive text/enum values, explicit declarations and source-to-report use.
It adds no external assessment, user session or ReqIF interoperability claim.

## Replay

Use the documented Java 21/GraalVM toolchain from the repository root:

```sh
make attribute-validate-verify attribute-format-verify attribute-compile-verify attribute-link-verify attribute-report-verify attribute-workflow-verify
make verify
scripts/run-ci-verification.sh
```

The authoritative gate includes all owning checks, old source/output compatibility,
structural schema validation, JVM/native parity and the following additions:

| Check | Independent observable outcome |
| --- | --- |
| check-attributes.py / attribute-schema.sh | Explicit selection, bounds, types, enums, requiredness, Unicode/SARIF locations and invalid publication |
| AttributeFormattingTest / check-attribute-format.py | Schema edit between replacements preserves external edit and remaining sources; CRLF-only normalization, comments/order and trace parity |
| AttributeCompilationTest / check-attribute-compile.py | Source/schema revision rechecks, no downgraded fields, exact compiled golden and an independent serialized fixture consumer |
| check-attribute-link.py | Scoped declarations, pins, conservative changes, malformed imports, explicit old/new promotion and parser-free work-item links |
| check-attribute-report.py | Exact escaped report, values/declarations/locations, recomputed findings, invalid input and output failure; existing migration/ReqIF rejection |
| check-attribute-corpus.py | 57 unchanged built-in requirements, four-file adoption, independent Unicode slices, invalid and Unicode artifact goldens |
| workflow.py | Author/validate/format/compile/plan/link/analyze/render the small example, source navigation and identical delete/rebuild |
| regressions.py | Twelve deterministic source-to-report seeds, exact model values, formatter preservation, value/schema edits and incomplete barriers |

Golden source/schema fixtures are checked in under examples/attributes. The golden
JSON files capture values, source/declaration locations, diagnostics and artifacts;
report.html captures derived presentation. Formatter checks compare directly with
independently expected authored bytes after CRLF normalization, avoiding a generated
formatter oracle. Historical goldens remain unchanged. Changes to these fixtures
need a reviewed semantic reason, not automatic golden refresh to silence a failure.

## Generation and mutations

Default seeds are 130800–130811; each creates two source files with 4–24 requirements,
required enums and optional literal Unicode/HTML-looking text. Replay one failure:

```sh
python3 experiments/0036-project-attributes/regressions.py --seed 130804
```

Failures retain inputs at `build/attribute-failure-SEED`. Minimize by removing unrelated
records/files while keeping the failing ID, declaration and its coverage row, then
rerun the failing public command. Preserve the original seed as a regression witness.
The default complete runner is bounded to 240 seconds, with 30-second child limits.

Three deliberate mutations are compiled in temporary classpath overlays: ignore
required attributes, accept invalid enums, and ignore whole-schema meaning in review.
Each unchanged implementation first passes its expected signature; each mutant must
compile and produce a different, incorrect signature. Uncompilable mutants do not
count as kills. [Recorded mutation results](results/mutations.json) are replayed
against actual behavior; tracked production sources are never changed by this check.
These are targeted behavioral witnesses, not a general mutation score or proof.

The [completion record](../../research/0068-attribute-workflow-verification.md) links
the final clean-checkout environment and gate logs. Earlier 1303–1307 summaries are working-checkout checks, not claims of hosted CI.
Generated artifacts and digests identify derived revisions, never authored identity.
