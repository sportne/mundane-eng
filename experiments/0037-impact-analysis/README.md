# Experiment 0037: Explained cross-artifact impact

Implements the bounded [impact contract](../../specification/0023-cross-artifact-impact-0.1.md)
using [checked-in logger source](../../examples/impact/README.md).

From the repository root, with the documented GraalVM environment:

```sh
make native-compile native-plan native-work native-impact
python3 experiments/0037-impact-analysis/workflow.py
```

The script stages source snapshots under `build/impact-example/source/`, checks each
compiler's JVM/native output, builds exact-pinned imports and compares query/report
output with checked-in goldens. Open `build/impact-example/IMPACT.md`; its links point
to the staged source snapshots. Change authoritative example files, then regenerate;
do not maintain edits in the staged copy or generated output.

The manually specified expected result is six candidates: DEV-001, ACT-STORAGE,
PLAN-LOGGER, TASK-STORAGE, TASK-PROCEDURE and TASK-PLAN. TASK-STORAGE remains Complete.
The independent power activity, baseline-scoped requirements and context-only issue
are absent. Activity-to-plan traversal does not fan back out to other activities.

## Reproducible checks

```sh
make impact-workflow-verify
```

- Public source-to-report JVM/native comparisons and exact JSON/text goldens.
- Legacy requirement output and attribute-aware output yield the same graph topology
  for the selected relationships; embedded schema/value data remain validated.
- Parser-free consumption without authored files or source parser classes.
- Malformed selections, unsupported formats, missing imports, wrong pins, bad
  attribute values, unknown scopes, declared build dependencies and forged reports.
- Sixteen seeded cyclic graphs checked against an independent fixed-point distance
  oracle, plus record permutations and the 10,000-node boundary.
- Actual closed OS pipe, source preservation and every rendered source link.
- Four isolated behavioral mutations: traversing generic links, ignoring depth,
  ignoring pins and bypassing report recomputation. Production files are unchanged.

Owning JVM tests additionally check graph coordinates, ambiguous single/multiple
scope resolution, detected snapshot changes, closed/partial/flush output errors,
report escaping and incomplete/forged results. These checks validate behavior;
they do not establish field usability, executed verification or claim approval.

The goldens are regression expectations, not authoritative engineering facts. The
text report golden uses `.txt` because its root-relative links belong to the staged
analysis root, not this fixture directory. Output versions evolve independently of
requirements, plans and work-item source formats.
