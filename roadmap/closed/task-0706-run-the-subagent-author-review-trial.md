# Task TC-0706: Run the Subagent Author and Review Trial

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0701",
    "TC-0702"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Independent author and review trial",
    "condition": "TC-0701 and TC-0702",
    "unlocks": "TC-1001",
    "statusNote": "Completion qualifier: Material scope deviation from the planned human case"
  }
}
```

## Question

Can fresh independent authors and reviewers understand, change, repair, trace,
and review source using only the written standard and tool documentation?

## Outcome

A two-author/two-reviewer subagent case records semantic convergence,
misunderstandings, repair behavior, documentation use, ordinary Git review, and
review-driven correction without claiming human learnability.

## Work

- Spawn at least two fresh-context author agents with no mundane-req design history and separate sealed repositories.
- Reuse or explicitly version the TC-0701 task package without revealing its oracle.
- Capture commands, statuses, diagnostics, questions, assistance, final source
  diffs, and every timing-evidence limitation.
- Assign a fresh-context reviewer to each author branch and complete an ordinary
  request-changes, follow-up-commit, and re-review cycle when findings occur.
- Compare semantic results with the frozen oracle and distinguish correctness, learnability, domain judgment, and tool usability.
- Use a separate evidence auditor to compare both cases with the oracle and
  TC-0702, and state every capture limitation.

## Acceptance evidence

- Both authors can complete or clearly fail every predefined task.
- The exact supplied package, task wording, assistance, deviations, author
  commits, reviews, follow-ups, and capture method are auditable.
- Observations are tied to concrete source, documentation, tool, editor, or Git interactions.
- Recommendations prefer documentation or diagnostics when grammar change is unnecessary.
- The result is described as an agent case, not human or statistical usability
  evidence.

## Out of scope

- A broad user study.
- Claiming systems-engineer usability, human effort, or normal editor/forge
  experience.
- Changing source or tools during the trial to hide friction.
- Evaluating unrelated watershed-domain expertise.

## Completion decision

If correct use depends on project history, treat that as a readiness gap before
1.0. Do not infer human learnability or broad usability from successful agents.

The project owner explicitly replaced the planned human-author case with
subagent authors and reviewers. Completed on 2026-08-30.
[Experiment 0023](../../experiments/0023-multi-subagent-author-review/README.md)
records two fresh authors completing all eight frozen tasks, two independent
reviews, review follow-up commits, and a separate oracle audit. Both final
sources format cleanly, validate as 61 requirements and 55 relationships, and
match the oracle's meaning-level criteria.

Review found no source-language, model, or mundane-req tool defect. It corrected
a minor blank-separator classification and exposed unreliable simulated timing
narratives in both author logs. The latter is a trial-method finding: elapsed
agent effort is unknowable from this capture.

Close the card with material scope deviation. This result adds independent
convergence and review evidence beyond TC-0702 but does not answer the original
human-usability question. TC-1001 must evaluate that absence explicitly rather
than treating it as favorable human evidence.

## References

- [TC-0701](task-0701-select-a-larger-corpus-and-trial-protocol.md)
- [TC-0702 proxy result](task-0702-run-the-independent-interpretation-proxy-trial.md)
- [Experiment 0012](../../experiments/0012-independent-author-trial/README.md)
- [Experiment 0023](../../experiments/0023-multi-subagent-author-review/README.md)
