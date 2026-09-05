# Task TC-1001: Audit Readiness for 1.0

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0704",
    "TC-0705",
    "TC-0706"
  ],
  "relations": [],
  "planning": {
    "stage": "10",
    "type": "Evidence audit",
    "condition": "TC-0704, TC-0705, TC-0706, and the model or ecosystem cards selected by evidence",
    "unlocks": "TC-1002",
    "statusNote": ""
  }
}
```

## Question

Has mundane-req earned a stable source-language compatibility promise, and which gaps remain material?

## Outcome

A traceable audit evaluates every Roadmap Stage 10 criterion against committed evidence and classifies gaps as blockers, explicit non-goals, or post-1.0 work.

## Work

- Review independent authoring and conformance evidence.
- Treat the deliberate substitution of subagents for the TC-0706 human-author
  case as an evidence limitation, not favorable human-usability evidence.
- Review validator, formatter, trace, operational-scale, and Git workflow results.
- Review language evolution and repository contract selection.
- Confirm dispositions for identity and every consequential model-pressure issue encountered in trials.
- Review native build, versioning, distribution, and clean-checkout documentation.
- Draft a complete intentional 1.0 exclusion list.

## Acceptance evidence

- Every readiness claim links to reproducible repository evidence.
- Tool existence alone is not treated as language maturity.
- Unknowns are not rewritten as non-goals merely to pass the audit.
- Language, CLI, Java API, and interchange stability are assessed separately.
- The audit recommends publish, defer with named blockers, or revise scope.

## Out of scope

- Writing the final 1.0 standard.
- Adding last-minute features.
- Promising support or certification not demonstrated.

## Completion decision

Advance only if the small source foundation is understood well enough to preserve. Otherwise keep provisional contracts and turn each material blocker into a bounded task card.

Completed on 2026-08-30. [Research 0031](../../research/0031-source-1.0-readiness-audit.md)
finds the 0.2 source syntax, semantic model, validity rules, and implementation-
conformance boundary sufficiently understood to proceed to a source-language-
only 1.0 publication decision.

The conditional recommendation is a no-feature, semantics-identical stable
successor: every conforming 0.2 source set remains conforming with the same
semantic value. Validator, formatter, and trace CLIs remain separately
versioned trials; Java APIs remain internal; ReqIF remains experimental.

No evidence blocker requires another language experiment before TC-1002.
Human learnability, sustained real-team use, incompatible-version coexistence,
independent ReqIF interoperability, companion carriers, and platform breadth
remain explicit residual risks or future evidence—not favorable readiness
claims. Proceed to TC-1002 without running conditional view, rendering, or
ReqIF cards merely to accumulate scope.

The independent-human criterion is not met: the authorized subagent trial
provides ambiguity pressure but not human learnability or effort evidence.
Formatter acceptance in normal human review and trace use in a real systems-
engineering workflow are also untested. TC-1002 must explicitly accept those
limitations for a narrow compatibility release or defer publication and create
bounded evidence cards.

The audit also found a concrete release blocker: `make verify` fails because
the formatter verification inventory omits valid source sets introduced after
Experiment 0011. This does not establish a formatter semantic defect, but the
omitted sets lack the formatter's maintained preservation evidence. TC-1002
must restore and pass the complete verification run before publication.

Resolved on 2026-08-30 during TC-1002: the formatter matrix now covers 29
source sets and 60 files, focused JVM/native verification passes, and the
complete `make verify` gate passes without a formatter behavior change. The
human-authoring, normal-review, and real-workflow evidence decisions remain
open.

## References

- [TC-0704](task-0704-measure-operational-scale.md)
- [TC-0705](task-0705-obtain-independent-conformance-evidence.md)
- [TC-0706](task-0706-run-the-subagent-author-review-trial.md)
- [Roadmap Stage 10](../0001-initial-roadmap.md#stage-10--decide-whether-to-stabilize-10)
- [Research 0031](../../research/0031-source-1.0-readiness-audit.md)
