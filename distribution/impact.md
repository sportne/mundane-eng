# Cross-artifact impact queries

Build the independent consumer with `make native-impact`. Its
[experimental contract](../specification/0023-cross-artifact-impact-0.1.md) answers
which selected artifacts might need review if a named artifact changes.

```sh
build/maintained/mundane-impact query --root . --from req:requirement:SYS-001 --depth 8 imports.json > impact.json
```

Check the exit status before consuming output. Exit 0 is a successful bounded query,
1 means invalid imports, graph or seed, and 2 means invocation, read, detected change
or output failure. Any failed output prefix is unusable.

The explicit `mundane-imports-0.1` manifest selects already compiled requirements,
verification plans and work items. Scope names must match references inside those
artifacts. Optional SHA-256 pins identify exact compiled bytes; IDs remain human
identity. All source coordinates use the specified root. Inputs from different
source roots require an explicitly prepared common layout; automatic rebasing is
not supported.

Impact follows parent-to-child decomposition, baseline/current coverage to
activities, activity-to-plan coverage membership, targets to addressing work, and
prerequisites to dependent tasks. It excludes generic links, supersession, evidence
resources and narrative citations. A work item's authored status does not suppress
review consequences or change automatically.

Results contain one deterministic shortest explanatory path per affected node.
Depth defaults to 8 (range 1..64); `truncated:true` means further selected nodes were
omitted by that bound. This is neither a comparison of old/new source nor a claim
that listed artifacts are invalid. Empty results only describe this selection and
policy. Existing verification review analysis supplies its separate revision
comparison; the impact command does not infer it from timestamps or digests.

## Derived report

```sh
build/maintained/mundane-impact view --root . impact.json > IMPACT.md
```

Save the report at the analysis root so its relative source links resolve. It shows
the seed, affected nodes, every step of each explanatory path, and selected compiled
revisions. A prominent notice identifies depth truncation. The renderer validates
embedded artifacts and recomputes results, rejecting forged paths or incomplete
analysis before writing a report. It does not reread source; linked files may have
changed since compilation. Regenerate both query and report for a fresh snapshot.
