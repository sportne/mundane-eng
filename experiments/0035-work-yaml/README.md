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


## Repository migration and replay

`python3 experiments/0035-work-yaml/migrate.py` replays the immutable baseline in
migration.json, compiles old and candidate source and checks recorded value/body
hashes and individual link retargeting. The completed `--write` conversion checked
all candidate values before writes; it refuses a second conversion, changed inputs
or existing outputs, and restores exact input bytes on local write failure. It is
a bounded repository migration recipe, not a general YAML formatter or transaction
across processes. There is no claim of atomicity against concurrent filesystem edits.

The inventory contains 81 cards plus the template. incoming-links.json records
mechanical link destinations changed in other documents. Current cards can evolve;
the immutable replay checks the conversion checkpoint, not a permanent source freeze.
`make work-backlog-verify` also checks current selection, links, graph and rebuild.
The schema suite loads every current YAML card and template independently.

`python3 experiments/0035-work-yaml/regressions.py` uses seeds 160900–160907 with
4–24 tasks and independently expected values/findings. Literal/escaped strings,
blank lines, indentation, Unicode, CRLF and line separators cover migration fidelity.
ID edits must leave references unresolved; duplicate keys must suppress all records.
Use `--seed NUMBER` to replay the reported seed. Each command has a 30-second limit;
no network service or contributor feedback is required. The existing three compiled
behavioral mutations remain in the owning work-item gate.
