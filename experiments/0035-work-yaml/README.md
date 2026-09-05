# YAML work-item verification

TC-1607 selects the specification; TC-1608 owns compiler conformance; TC-1609 owns
repository migration. These extend the earlier work-item checks, whose Markdown
examples remain compatibility fixtures.

Run `make work-yaml-verify` with the repository's documented Java/GraalVM toolchain.
The target uses the pinned independent YAML 1.2 loader and Draft 2020-12 validator
already installed for requirement schema checks. Public JVM/native commands verify
literal/folded/quoted strings, CRLF normalization, defaults, forbidden YAML features,
source marks, explicit manifest selection, schema/domain failures, legacy imports,
parser-free consumers and failed output streams. No production requirement parser
is reused. Source comments are presentation, body strings remain opaque.

`golden/legacy-compiled.json` is the unchanged output from the previous work-item
compiler; it checks backward reading independently of the current legacy compiler.
The older example goldens now identify the current compiler/CLI version; semantic
items, graph edges/findings and source snapshots were checked unchanged before
those provenance updates. Historical experiment logs remain untouched.
