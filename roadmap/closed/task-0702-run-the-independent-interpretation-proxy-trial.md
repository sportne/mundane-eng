# Task TC-0702: Run the Independent-Interpretation Proxy Trial

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0701"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Independent-interpretation trial",
    "condition": "TC-0701",
    "unlocks": "TC-0704",
    "statusNote": ""
  }
}
```

## Question

Can a fresh independent interpreter understand, author, repair, and trace source using only the written standard and tool documentation?

## Outcome

An AI-agent proxy trial records successful tasks, misunderstandings, repair behavior, and documentation or language friction without claiming human usability.

## Work

- Provide only the published source and tool contracts plus the selected corpus.
- Ask the participant to read, add, change, move, format, validate, trace, and review requirements.
- Capture errors and questions without coaching them toward hidden conventions.
- Compare their semantic results and Git diffs with the protocol expectations.
- Classify each issue before proposing a fix.

## Acceptance evidence

- The participant can complete or clearly fail each predefined task.
- All assistance and deviations from the protocol are recorded.
- Observed problems are tied to concrete source or tool interactions.
- The result distinguishes learnability from correctness.
- Recommendations prefer documentation or diagnostics when grammar change is unnecessary.

## Out of scope

- A broad user study.
- Changing tools during the trial to hide friction.
- Evaluating domain expertise unrelated to mundane-req.

## Completion decision

If independent use depends on oral project history, treat that as a material readiness gap before considering 1.0.

Scope correction: the available participant was a fresh AI agent, not a human
engineer. This card records that bounded independent-interpretation case. The
original human-usability question is preserved in TC-0706 rather than treating
proxy behavior as engineer-usability evidence.

Completed on 2026-08-29. [Experiment 0012](../../experiments/0012-independent-author-trial/README.md)
records a fresh AI-agent proxy completing every frozen read, add, change, move,
repair, format, validate, trace, review, branch, and commit task with no project
history, questions, or assistance. The final patch exactly matches the hidden
semantic oracle and validates at 61 requirements and 55 relationships.

Written material was sufficient for this independent interpretation, but the
result is not human or statistical usability evidence. Retain the grammar;
carry documentation volume and multi-record move noise into later evidence.

The protocol deviation from unavailable raw session capture is recorded; final
correctness and preserved Git evidence were independently verified. Proceed
without a grammar change, but do not use this result to satisfy TC-0706.

## References

- [TC-0701](task-0701-select-a-larger-corpus-and-trial-protocol.md)
- [Normative 0.2 standard](../../specification/0005-mundanereq-source-language-0.2.md)
