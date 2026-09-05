# Research 0061: YAML work-item compiler verification

TC-1608 completed 2026-09-05.

Implemented specification 0019 in an independent work-item YAML adapter using the
already pinned SnakeYAML Engine library. The event pass rejects disallowed YAML
features before composition; node conversion detects duplicate keys and wrong scalar
types before semantic validation. In testing, composition expanded a merge key, so
merge-key detection was moved to the event pass. The regression now rejects it.
Missing optional lists/annotations normalize to documented empty values; explicit
nulls fail. Narrative is the exact decoded string, with ordinary YAML newline rules.

Manifest 0.2 selects YAML 0.2; manifest 0.1 still selects Markdown source 0.1. New
output 0.2 permits genuine YAML declaration points; old output 0.1 keeps its fixed
coordinates. Current tool/CLI declarations advance independently; analysis remains
0.1 with unchanged graph meanings. Source adapters are absent from consumer tests.

Observed checks in the local Java 21/GraalVM environment:

- `make native-work`: all 18 JVM groups and native build passed.
- `scripts/check-work-yaml.sh`: independent requirement schema setup/check,
  six scalar styles under LF/CRLF, exact decoded strings, optional-field defaults,
  32 invalid profiles, source marks, explicit profile failures, old artifact reading,
  YAML imports, parser-free analyze/view and real broken output passed. Independent
  YAML 1.2/Draft 2020-12 work schema positive/negative checks passed.
- `python3 scripts/check-work-items.py build/maintained/mundane-work`: existing
  Markdown compiler, serialized linking, golden outputs, source-free views and
  stream failures passed with the current runtime.
- `python3 experiments/0034-work-items/regressions.py`: eight existing seeded
  scenarios and all three compiled behavioral mutations passed/killed respectively.

Old compiled example values/source snapshots and graph edges/findings were compared
before updating only producer metadata and derived provenance in their current
expected outputs. The previous compiled file is retained unchanged as a legacy
consumer fixture in experiment 0035. Historical recorded results were not rewritten.

No repository card migration or clean-checkout verification is claimed here;
TC-1609 owns those outcomes. No new narrative metamodel, formatter, external tracker
or requirement-language changes were implemented.
