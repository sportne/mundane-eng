# Operational Corpus Selection

Status: Decided

Date: 2026-08-29

Roadmap task: [TC-0701](../roadmap/closed/task-0701-select-a-larger-corpus-and-trial-protocol.yaml)

## Decision

Use the original 60-requirement corpus and frozen protocol in
[Experiment 0011](../experiments/0011-operational-corpus/README.md). The search
found licensed public requirements but no import that satisfied every trial
constraint without either inventing trace semantics, broadening beyond
requirements, accepting avoidable license/model coupling, or revisiting the
explicit StrictDoc provenance boundary.

This is not evidence that suitable public corpora do not exist. It is a bounded
selection decision from four especially relevant candidates. Exact revisions,
licenses, structural lessons, and rejection reasons are retained in the
experiment provenance record so another contributor can challenge the choice.

The fallback makes the corpus legally simple and semantically honest at the
cost of independent domain authorship. TC-0702 therefore tests independent use
of mundane-req, not realism or completeness of the watershed requirements.
