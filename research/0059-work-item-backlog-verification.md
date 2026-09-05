# Research 0059: Work-item backlog integration

Date: 2026-09-05. TC-1606. Complete integration evidence.

The repository's 78 cards now use the selected structured Markdown profile, with
human IDs/headings and narrative suffixes preserved at conversion. A replayable
[migration inventory](../experiments/0034-work-items/migration.json) binds each
transformation to commit 59e062c. Prerequisite qualifications and the exceptional
completion/current-disposition headers remain visible in planning annotations.
The first conversion preflight identified those two extra headers; it stopped on
unknown metadata rather than dropping it. The recipe now preflights every card
before writing, explicitly preserves both fields, and replays all 78 conversions.

Card facts are authoritative. `roadmap/work-items.json` selects paths only; the
root [derived index](../WORK-ITEMS.md) is rebuilt from compiler/analyzer output.
The existing task index retains planning narrative and links to the generated view
instead of duplicating status tables. Four execution cards gained explicit local
code/specification evidence citations. Body links were not inferred as typed edges.

The owning tests cover typed links to requirements, verification plans, activities
and separately scoped work items. Eight replayable seeds independently check values,
prerequisite findings and ID-correction failures. Three compiled implementation
mutations exercise partial publication, prerequisite suppression and ignored pins.
Their [recorded results](../experiments/0034-work-items/results/mutations.json)
show all three killed by passing baseline witnesses. This is bounded regression
evidence, not a general coverage metric or user adoption study.

CI checkout now fetches history because migration replay reads its immutable ancestor;
the [checkout action input](https://github.com/actions/checkout/blob/v4/action.yml)
defines fetch-depth 0 for this purpose. Current native packaging behavior and
requirements/plan source contracts are unchanged. The work tool has its own version
declarations and build target; its consumer runs without source-parser classes.

The complete `scripts/run-ci-verification.sh` gate passed from an initially clean,
detached checkout at `0f470532866de9bc94596370e2b66deba2ba249f`, with no preexisting
build output. The [environment](../experiments/0034-work-items/results/clean-environment.txt),
[full gate log](../experiments/0034-work-items/results/clean-verify.log),
[failure-propagation log](../experiments/0034-work-items/results/clean-failure-propagation.log)
and [exit status](../experiments/0034-work-items/results/clean-status.txt) record the
actual run: 18 JVM groups, existing native/package/requirement/plan checks, the new
compiler/link/view checks, 78-card migration and identical rebuild, eight seeds and
three killed compiled mutations. Invalid-example and omitted-schema-target injections
both failed the authoritative gate and restored their input bytes. The wrapper
returned 0 and the verification checkout remained clean. Recorded elapsed time was
220.45 seconds on Ubuntu 24.04 x86-64, GraalVM CE 21.0.2, GCC 13.3.0,
Python 3.12.3 and Ruby 3.2.3.

Final closeout changes only evidence, task status/path, planning documentation and
the regenerated derived index; runtime/build/test inputs match that verified
candidate. Card TC-1606's normal completion append and relative-link rebasing follow
the recorded conversion checkpoint. All other converted card narratives remain
byte-identical to that checkpoint. No hosted result, external-tracker fidelity,
requirement satisfaction or user adoption is claimed.
