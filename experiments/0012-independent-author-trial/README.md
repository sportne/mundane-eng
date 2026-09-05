# Experiment 0012: Independent-Author Trial

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0702](../../roadmap/closed/task-0702-run-the-independent-interpretation-proxy-trial.yaml)

## Result

A fresh AI-agent participant with no project conversation history completed all
eight frozen tasks from the supplied source standard, tool contracts/guides,
native executables, corpus, and task sheet. It asked no questions and received
no assistance. Its final source is formatter-clean and validates as 61
requirements with 55 relationships in six files.

The participant correctly read and traced existing source, added a requirement,
made a coordinated retention decision, moved a record without semantic change,
created and repaired a dangling reference from the validator diagnostic, ran
all three tools, classified its ordinary diff, and committed on an ordinary Git
branch. The [assessment](assessment.md) distinguishes correctness from
learnability and records the limitations of an AI proxy.

## Durable evidence

- [exact participant prompt and invocation/final response](participant-invocation-and-response.md);
- [frozen tool/source manifest](administrator-manifest.txt);
- [participant-authored chronology](participant-log.md);
- [reconstructable source patch](source-change.diff); and
- [oracle comparison and friction classification](assessment.md).

No temporary binary, generated report, index, database, or trial Git repository
is authoritative. Hashes identify every sealed input; the source patch
reconstructs the result from the corpus at Git tag
`operational-trial-protocol-0.1`. No raw participant session transcript was
available, and the assessment records that evidence deviation explicitly.

## Decision

The result provides positive AI-proxy independent-interpretation evidence
without justifying a grammar change. It also exposes two questions for later
evidence: whether a human needs a shorter learning path than the complete
1,633-line package, and how record moves compare between subject-file and
one-record layouts. TC-0706 retains the human-usability question.
