# Experiment 0023: Multi-Subagent Author and Review Trial

Status: Completed

Date: 2026-08-30

Roadmap task: [TC-0706](../../roadmap/closed/task-0706-run-the-subagent-author-review-trial.yaml)

## Result

Two fresh-context author agents independently completed the frozen eight-task
operational-corpus trial from identical sealed source, documentation, and
native-tool packages. Two other fresh-context agents reviewed the ordinary Git
branches. A fifth agent audited both results against the withheld oracle.

Both final source snapshots are formatter-clean and validate as 61
requirements, 55 relationships, and six files. Each author correctly added the
queue requirement, changed record retention from 30 to 90 days, preserved the
semantics of a moved record, repaired a temporary dangling relationship, and
answered the frozen trace questions. Their different prose and destination
files remain semantically acceptable because prose judgment is project/domain
work and file placement is nonsemantic.

The ordinary requirements diffs were sufficient for both independent reviews.
Review found no grammar, model, or mundane-req tool defect. It did find one
minor diff-classification error and, more importantly, unreliable simulated
timing narratives in both participant logs. Follow-up commits preserved the
original evidence, corrected the logs, and received final approval.

## Decision

No source-language or tool change is justified. TC-0706 is complete under the
project owner's explicit decision to substitute a two-author/two-reviewer
subagent case for the planned human-author case.

This is a material scope deviation, not human-usability evidence. It supports
independent written interpretation, semantic convergence, ordinary diff
review, and review-driven correction. It does not establish human reading
burden, learnability, elapsed effort, editor or forge experience, domain-
engineer judgment, or broad usability. TC-1001 must carry that absence as an
explicit readiness risk rather than treating it as resolved.

## Durable evidence

- [Protocol and capture](protocol-and-capture.md)
- [Subagent invocations](invocations.md)
- [Administrator manifest](administrator-manifest.txt)
- [Author A final log](author-a-log.md)
- [Author B final log](author-b-log.md)
- [Author A requirements patch](author-a-source.diff)
- [Author B requirements patch](author-b-source.diff)
- [Author A complete post-baseline commit chain](author-a-history.patch)
- [Author B complete post-baseline commit chain](author-b-history.patch)
- [Review record](reviews.md)
- [Reviewer A transcript](reviewer-a-transcript.md)
- [Reviewer B transcript](reviewer-b-transcript.md)
- [Evidence-auditor report](auditor-report.md)
- [Assessment](assessment.md)
- [Evidence checksums](SHA256SUMS)

The temporary trial repositories and native executables are disposable. The
manifest identifies the sealed inputs, the requirements-only patches
reconstruct the authored source changes from the committed corpus, and the
format-patch streams preserve every post-baseline author and follow-up commit.
