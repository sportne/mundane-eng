# Next Ecosystem Tool Prioritization

Status: Complete; no additional tool selected

Date: 2026-08-29

Roadmap task: [TC-0901](../roadmap/closed/task-0901-prioritize-the-next-ecosystem-tool.yaml)

## Decision question

After the validator, formatter, trace tool, integrated Git trial, operational
trial, and model-pressure studies, which one additional independent capability
removes the most consequential demonstrated friction?

## Evidence baseline

- [Experiment 0010](../experiments/0010-integrated-toolchain-trial/README.md)
  found ordinary diffs clear and the three tools independently useful. It left
  timing-budget consistency outside their responsibilities but did not show a
  recurring rule.
- [Experiment 0014](../experiments/0014-operational-scale/README.md) found
  low-millisecond in-memory operation and no need for persistent state.
- [Experiment 0019](../experiments/0019-diagnostic-presentation/README.md)
  selected a future bounded validator option but found no consumer for a
  machine-readable diagnostic contract.
- Experiments [0016](../experiments/0016-identity-continuity/README.md),
  [0020](../experiments/0020-allocation-model/README.md), and
  [0021](../experiments/0021-glossary-symbols/README.md) retained simpler source
  and Git behavior rather than selecting new tools. Experiment 0020 permits a
  separate project vocabulary but selects no standard carrier or maintained
  checker.
- Experiments [0017](../experiments/0017-verification-evidence/README.md) and
  [0018](../experiments/0018-safety-classification/README.md) selected model
  concepts but left their companion carriers provisional.
- [Experiment 0022](../experiments/0022-trace-policies/README.md) separated
  policy from language validity but found no policy demonstrated to recur.
- [Experiment 0012](../experiments/0012-independent-author-trial/README.md)
  records successful AI-proxy use, not human usability. TC-0706 remains the
  explicit human-evidence gap.

## Candidate comparison

| Candidate | Observed workflow and evidence | Authoritative input / output | Existing infrastructure | Cost and disposition |
| --- | --- | --- | --- | --- |
| Prose or project-policy linter | Timing consistency was outside validation; allocation vocabulary and downward coverage worked as project checks, but none recurred | `.mreq` plus explicit policy source / disposable diagnostics | Review, CI scripts, validator, and trace already expose source and graph facts | A stable rule vocabulary, scope, waivers, and false-positive policy would cost more than the observed use warrants. Defer. |
| Renderer or document generator | No delivery or review workflow has required a rendered artifact; TC-0807 remains gated | `.mreq` and perhaps ordering source / disposable HTML, PDF, or matrix | Editor, forge diff, and text search already support current review | Presentation and ordering decisions would be invented before use. Defer until a concrete delivery asks for TC-0807/0903. |
| Semantic baseline comparator | Ordinary Git diffs exposed changed numbers, added records, reallocation, and ID/link edits clearly | Two repository snapshots / disposable semantic report | Git diff, history, tags, validator, and trace | Semantic matching policy adds identity and compatibility questions without measured review failure. Defer. |
| Verification or assessment analyzer | Verification and safety studies selected conceptual facts, but both carriers remain provisional and have one experiment each | `.mreq`, exact baseline, companion source / coverage or assessment report | Focused experiment queries and ordinary text inspection | Implementing now would freeze unsettled carrier and satisfaction semantics. Run another real workflow first. |
| Editor or language-server support | The AI proxy completed all tasks without assistance; no human-author evidence identifies completion, navigation, or diagnostic friction | `.mreq` / ephemeral editor diagnostics and navigation | Ordinary editor, search, native CLI, and Git | Protocol complexity and editor coupling are unjustified without human friction. Defer until TC-0706. |
| ReqIF converter | Bounded schema-valid self-roundtrip works; no independent tool or exchange partner has tested edits and losses | `.mreq` / explicit external interchange artifact | Experiment 0006 probe and ReqIF ecosystem | Interchange may be important, but a maintained converter is premature. TC-0902 is the bounded next experiment when an independent implementation is available. |
| Machine-readable diagnostics or analysis | Complete JSONL was feasible, but no CI consumer selected fields or format | `.mreq` / disposable JSONL, SARIF, or forge annotations | Human text, redirected artifacts, CI-native formats | Freezing provisional categories for an imagined consumer creates compatibility cost. Defer. |

No candidate currently clears the roadmap's completion gate. This is not a
claim that these tools lack value; it is a claim that the repository does not
yet contain evidence sufficient to choose one.

## Selected next experiment

Select no ecosystem-tool implementation and do not unlock TC-0904. The next
bounded evidence milestone is existing
[TC-0706](../roadmap/closed/task-0706-run-the-subagent-author-review-trial.yaml).
Its primary question is whether a systems engineer can understand, author,
repair, trace, and review the source using the written material and current
tools.

Stop when one consenting participant with relevant background has completed or
clearly failed every frozen task and the repository records supplied material,
assistance, commands, final diff, semantic comparison, task effort, and concrete
friction. Classify each issue first as documentation, diagnostics, editor/Git,
language, or tool responsibility. That evidence can reopen prioritization with
a demonstrated consumer and one primary responsibility.

TC-0902 remains the next bounded interchange experiment when access to an
independent ReqIF implementation and a credible exchange workflow exist. It is
not selected as the next maintained tool.

## Decision

Build no fourth tool now. Complete the human-author trial before revisiting the
portfolio. Existing documentation or infrastructure is not declared sufficient
in advance; the trial is how the project will determine where it is
insufficient. Keep source authoritative, avoid persistent services, and require
future candidates to solve one observed workflow before implementation.

## Subsequent scope decision

On 2026-08-30 the project owner explicitly replaced the planned human case with
the [Experiment 0023](../experiments/0023-multi-subagent-author-review/README.md)
two-author/two-reviewer subagent trial. That experiment adds independent
interpretation and review evidence but cannot provide the human effort,
completion, navigation, editor, or forge evidence that this research named as
the condition for reconsidering editor/LSP support.

Therefore Experiment 0023 does not positively satisfy the original editor-tool
gate and does not select a fourth tool. The project has chosen not to collect
that human evidence at this stage. TC-1001 must decide whether the resulting
uncertainty blocks 1.0, belongs outside the intended compatibility promise, or
requires a later bounded task. TC-0904 remains locked.

## Reopened by end-to-end workflow evidence

On 2026-08-30 [Experiment 0024](../experiments/0024-vaccine-monitoring-pilot/assessment.md)
executed the second verification companion workflow requested by this decision.
Across 57 requirements and two baselines, manual checks were needed for
requirement/activity references, baseline binding, coverage, and staleness.
The retention change also showed that whole-source-set binding creates
irrelevant assessment churn.

This evidence selects a focused verification-plan analyzer, not a generalized
policy tool. [TC-0905](../roadmap/closed/task-0905-define-verification-analyzer-contract.yaml)
must compare revision-binding granularities and freeze a bounded trial contract.
Only a successful TC-0905 unlocks TC-0904 implementation.
