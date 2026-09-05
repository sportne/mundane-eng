# Multi-Author Layout Decision

Status: Decided for current guidance

Date: 2026-08-29

Roadmap task: [TC-0703](../roadmap/closed/task-0703-run-the-multi-author-and-layout-trial.yaml)

## Decision

[Experiment 0013](../experiments/0013-multi-author-layout-trial/README.md)
demonstrates that both tested layouts preserve identical requirement semantics
and remain manageable with ordinary Git. One-record files materially improve
the selected move-versus-edit case through Git's existing rename detection, but
they do not avoid true same-line or nearby same-record conflicts and they expand
the corpus from six files to 60.

Keep file granularity non-semantic. Document the tradeoff as project guidance,
not language law. Neither a semantic merge engine nor a source manifest is
justified. The experiment measures deterministic source operations and conflict
behavior, not human author effort; TC-0706 retains that evidence gap.
