# Task-card index and workflow

[WORK-ITEMS.md](../WORK-ITEMS.md) is the generated index of all selected cards,
statuses, prerequisites and typed relationships. The [roadmap](0001-initial-roadmap.md)
provides direction; neither document duplicates the cards' authoritative status.

## Source conventions

Cards use [work-item YAML](../specification/0019-work-items-yaml-0.2.md) and the
[task-card template](task-card-template.yaml). Human IDs and filenames are maintained
explicitly. The checked-in [selection](work-items.json) lists current source paths.
The literal `body` string holds narrative; machine relationships belong in the typed
fields. Stage numbers group existing work and are not a release schedule.

## Updating cards

1. Update the authoritative YAML metadata and narrative. Preserve IDs and explain
   material scope changes. Reuse existing cards where their work already applies.
2. Record completion evidence before setting `Complete`. Move completed cards into
   `closed/`, update the selection and repair incoming links.
3. Regenerate the derived index with `make work-index`.
4. Run `make work-backlog-verify` and the checks appropriate to the change.
5. Commit each completed task separately, including its source and derived index.

A `Conditional` card requires its stated evidence or consumer, even when all listed
dependencies are complete. Use the established `Superseded` status and explicit
incoming `supersedes` relationship when a successor replaces a card. Historical
completed cards remain evidence for their recorded scope.
