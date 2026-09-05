# Research 0058: Derived work-item view verification

Date: 2026-09-05. TC-1605.

Implemented `mundane-work view` as a deterministic Markdown projection of complete
analysis. It revalidates every embedded supported artifact and recomputes edges,
prerequisite findings and the referenced resource inventory before rendering.
It does not trust a supplied complete flag or silently omit changed findings.

The view displays source-linked IDs, kinds, titles, statuses, dependencies,
unfinished prerequisites, planning qualifications, declared relationships, reverse
navigation and exact snapshot provenance. Source links are encoded and root-relative;
imported source roots are not guessed. Markdown body prose remains in the source
card; the view does not execute or render that prose as HTML. Generated status and
relationships are explicitly derived.

Actual local JVM/native tests passed the [view golden](../experiments/0034-work-items/golden/view.txt),
identical rebuild after deleting the input, unknown/incomplete/altered analysis,
changed findings, omitted edges, invented resources, invalid embedded statuses,
array-valued input, HTML/Markdown-looking titles and real broken output after a
prefix. The view ran from a temporary root containing only serialized analysis,
without its authored source files. The owning 18-group JVM suite also passed.

No packaging or publishing service was introduced. TC-1606 adopts the source
profile and generated index in this repository; source cards remain authoritative.
Full clean-checkout evidence belongs to that integration card.
