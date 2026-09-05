# Research 0062: YAML backlog migration and verification

TC-1609. YAML authoring follows specification 0019 and Research 0060.

The repository now selects 81 YAML task cards. The template is also YAML. A body
literal retains the original decoded narrative, including initial/final newlines,
Markdown, indentation and code examples. The migration compares the complete values
of every candidate before writing. Original IDs, titles, statuses, dependencies,
relations and planning qualifications survive; ordinary subsequent execution status
and completion updates are separate from the migration checkpoint.

The explicit exception is link retargeting: 98 destinations across 60 cards were
changed to the corresponding YAML paths. Another 46 documents had mechanical
incoming-link updates. The inventory records each destination change and source,
converted-source, body and semantic-value hashes. It names the immutable input
commit; replay compiles both representations and compares all expected values.
No prose relationships are inferred, and no competing Markdown source cards remain.
Historic completed cards keep their original evidence and IDs even though their
source filenames now end in .yaml. Compatibility Markdown examples remain outside
the live backlog.

Current authoring documentation, template, explicit manifest and derived index now
point to YAML. Generic schema validation loads every card and the template with an
independent YAML 1.2 implementation. The compiler remains the authority for profile
and domain checks; analysis owns dependency/relationship checks. Generated output
is disposable. Requirements and plan formats are unaffected.

## Verification evidence

The immutable verification candidate is
`22a4521a37afffb08d70be6b8b9a6c7cf4fff41a`, retained by the local
`codex/yaml-work-verification-evidence` branch. Verification runs in a separate
checkout with no preexisting build directory. Final completion notes and the
regenerated index are recorded after that run; implementation/check inputs are
compared against the candidate before completion.

Focused checks passed: migration replay, all 81 actual semantic records versus the
checkpoint (allowing the executing card's In progress status), independent schema,
current graph/index/relative links, deterministic delete/rebuild, and eight YAML
workflow seeds. Existing work-item regression coverage remains active, including
three compiled behavioral mutations. The corrected full gate passed as recorded below.

The first isolated gate attempt exposed an old linker isolation check that compiled
all engineering sources. It therefore pulled in the unrelated work-item YAML adapter.
The check now compiles only engineering/artifacts into a fresh class directory,
with no source parsers or YAML library. Its 57-edge/18-invalid-case matrix passes.
The corrected candidate above includes this test-boundary correction.


## Completed authoritative gate

On 2026-09-05, `scripts/run-ci-verification.sh` completed with exit 0 from the clean
candidate checkout. All 18 JVM groups, native builds and existing requirement,
formatting, compilation, linker, verification, report, parser-recovery and SARIF
checks passed. Work-item legacy/YAML public parity, invalid-profile and schema
checks, serialized consumers, migration replay, complete graph/index rebuild,
eight legacy seeds, eight YAML seeds and three killed work-item mutants passed.
Both injected CI faults failed at the expected targets and restored exact inputs.
The checkout remained clean.

Recorded evidence:

- [Environment and candidate](../experiments/0035-work-yaml/results/clean-environment.txt)
- [Full make verify output](../experiments/0035-work-yaml/results/clean-verify.log)
- [CI failure propagation](../experiments/0035-work-yaml/results/clean-failure-propagation.log)
- [Wrapper exit status](../experiments/0035-work-yaml/results/clean-status.txt)

Only terminal control sequences and trailing output whitespace were removed from
copied logs. Final closeout adds this evidence, the TC-1609 completion record/path
and the regenerated index. Source/build/check inputs match the verified candidate;
final current selection, graph, exact rebuild, relative links and diff whitespace
are checked again after that closeout. No hosted CI run, push or release is claimed.
