# Experiment 0025: YAML requirement source comparison

Status: Complete bounded comparison; candidate syntax is nonnormative

Task: [TC-1105](../../roadmap/closed/task-1105-compare-yaml-and-custom-requirement-source.yaml)

Decision: [Adopt a constrained YAML target through successor work](../../research/0033-yaml-source-representation-decision.md)

## Question and method

Compare current custom source with ordinary YAML carrying the same requirement
model. Eleven requirements in four files per format exercise all built-in fields,
multiple records per file, single-record files, paragraph order, opaque LaTeX,
Unicode, links, and thirteen comments. Ten records are copied from repository
fixtures at commit `ea463f8`; one is explicitly synthetic. Selection and source
hashes are in [provenance.json](provenance.json). This is an authored comparison,
not independent usability evidence or a performance benchmark.

- [Profile and field mapping](profile.md)
- [Observed comparison and limitations](comparison.md)
- [Clause-level specification outline](specification-outline.md)
- [Completion verification](verification.md)
- [Machine-readable schema](schema.json)
- [Expected pressure cases](invalid/cases.json)
- [Recorded core results](results/summary.json), [normalized semantics](results/semantics.json),
  [parser defaults](results/parser-defaults.json), [native parity](results/native-summary.json),
  and [environment](results/environment.json)

## Reproduce

From the repository root, use Java/Javac 21, Python 3.12 with venv support, Git,
and network access for setup. Setup downloads pinned Python dependencies and the
SHA-256-checked SnakeYAML Engine 3.1.1 jar. No production dependencies are added.

```sh
experiments/0025-yaml-source-comparison/setup.sh
experiments/0025-yaml-source-comparison/run.sh
COMPARISON_NATIVE_IMAGE=/path/to/graalvm-21/bin/native-image \
  experiments/0025-yaml-source-comparison/native-check.sh
```

After setup the core replay needs no network. It compares fresh results against
recorded bytes and fails on drift. Native replay builds the experimental Java
parser/emitter and compares its outputs with the JVM. The Java classes are built
with `--release 21 -Xlint:all -Werror`. Optional `COMPARISON_JAVA` and
`COMPARISON_JAVAC` select the Java tools. See the environment record for the exact
Linux/WSL, Java 21.0.12 and GraalVM CE 21.0.2 configuration actually exercised.

Maintainers may deliberately refresh evidence with `run.sh --record` and
`native-check.sh --record` after reviewing changes. Regenerate the fixed fixture
selection with the following commands; this is an experiment generator, not a
migration utility for arbitrary projects:

```sh
build/yaml-comparison/venv/bin/python -B experiments/0025-yaml-source-comparison/prepare-fixtures.py
build/yaml-comparison/venv/bin/python -B experiments/0025-yaml-source-comparison/prepare-invalid.py
```

`capture-environment.py` separately records local tool versions and parser scalar
resolution. Environment data is informational and is not required to match a
second machine byte for byte. Build products, packages, temporary files and native
binaries stay under ignored `build/yaml-comparison/`.

Custom copies use `.mreq.fixture` so directory discovery and the maintained
formatter inventory do not ingest duplicate experimental IDs. The harness passes
them explicitly to the actual maintained interpreter and formatter. Deliberate
invalid UTF-8 files and Git conflict outputs are negative evidence, not broken
repository source files. Existing conformance inventories are unchanged.

## Coverage boundary

The core replay checks 11 equivalent records, 28 pressure cases and 12 matched
workflow outcomes, including four Git merge outcomes. It also checks reversed
input order, combined/reordered records and cross-file duplicate IDs. Rich Java
emitter failures are captured as findings; only the conservative path and Python
round-trip path are asserted to preserve the tested comments and be idempotent.

The 43 JVM/native comparisons cover parsing and emission only. JSON Schema and
domain validation run in Python; no native Java schema validator, maintained YAML
CLI, editor integration or migration tool has been implemented. The small corpus
is not exhaustive conformance evidence. The current normative 0.2 language,
production tools, tests, Makefile and CI remain unchanged.
