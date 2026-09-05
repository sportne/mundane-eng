# Experiment 0016: Identity Continuity

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0801](../../roadmap/closed/task-0801-test-identity-continuity.yaml)

## Question

Does correction of a human-facing requirement ID require continuity semantics
beyond an atomic Git change, and would either an explicit continuity assertion
or a second durable identity justify its cost?

## Workflow and controls

The experiment extends the real `LPC_KIAS_0` correction from Experiment 0004
with one controlled typo correction that has more consumers:

```text
SYS-MONITOR-SMAPLE-001 -> SYS-MONITOR-SAMPLE-001
```

Both adjacent baselines contain three conforming requirements and two incoming
decomposition links. The same correction updates an internal verification-plan
reference. A supplier reference remains stale outside the baseline.

The Git-only candidate is supplemented by the real annotated baselines from
Experiment 0004, where an atomic ID correction and companion-plan update are
available to ordinary `git show` and `git diff`. That prior history supplies the
history evidence; the controlled three-requirement fixture holds the candidates
and consumers constant.

[`run.sh`](run.sh) validates both baselines, compares equivalent trace results,
proves that the stale ID fails precisely in Baseline B, records ordinary source
and consumer diffs, exports both complete graphs through Experiment 0006's
actual ReqIF adapter, and exercises these alternatives:

1. **ID-only replacement:** update every in-repository reference atomically and
   rely on Git history to explain editorial intent.
2. **Continuity assertion:** add a separate predecessor/successor assertion
   bound to named baselines and their source-content digests, with a reviewed
   `same-requirement` decision, policy-recognized authority role, and basis.
   Conflicting, inapplicable, unauthorized, and content-stale assertions are
   rejected by the experimental resolver.
3. **Durable identity:** bind both human IDs to one stable opaque ID and use that
   ID for a supplier reference and candidate ReqIF transport identity. All three
   requirements and both relationships participate in the transport trial.

The source and candidate artifacts are deliberately plain text. The continuity
and durable-ID candidates are experimental companion models, not selected
`.mreq` syntax.

## Results

### Snapshot and trace semantics

Both baselines validate as three requirements and two relationships. Correcting
the ID and both incoming links leaves the graph shape unchanged, but the 0.2
semantic model correctly reports the old ID missing in Baseline B. Git intent
does not silently change snapshot identity.

The ordinary [requirements diff](results/requirements.diff) is three one-token
changes. The [verification-plan diff](results/verification-plan.diff) is one
corresponding token. This is understandable as one reviewed atomic change, while
a semantic baseline comparator must still call it removal plus addition.

### Stale external consumer

The supplier's old human ID is unresolved in the new snapshot. Git-only history
can help a person diagnose it but is neither shipped in every exchange nor a
machine-readable continuity assertion. The recorded Experiment 0004 history
shows both identifiers in an ordinary atomic diff; it does not change either
snapshot's semantics.

The explicit continuity artifact resolves that exact old ID to the new ID when
its baseline labels and content digests, `same-requirement` decision, recognized
authority role, and basis are present and unambiguous. Conflicting,
wrong-target-baseline, unauthorized-role, and changed-content fixtures fail.
It therefore adds real capability for a consumer that receives and trusts the
artifact. It also creates new policy questions: whether assertions are
cumulative, how chains compose, who may hold the named authority role, and what
split, merge, reuse, or reversal mean.

The durable consumer receives the opaque ID at Baseline A and mechanically
resolves it to the corrected human ID through Baseline B's binding. The same
candidate is unable to repair the separate human-ID-only reference after the
fact. Universal durable IDs would burden every requirement to improve a
workflow demonstrated for one rare correction.

### ReqIF mapping

The actual Experiment 0006 adapter exports and imports both complete baselines:
three objects and two relationships each. Under its current bounded profile,
deriving object and relationship XML identifiers from human IDs changes three
transport identifiers: the corrected object and both incoming relationships.
The continuity assertion can inform a cooperating update adapter but ReqIF does
not make that project assertion true automatically.

The candidate durable-ID adapter rewrites every object, relationship, and
reference transport identifier from complete durable bindings. Its Baseline A
and B identifier manifests are equal, and reversing the candidate policy lets
the existing strict importer recover the same semantic inventories. This proves
the mechanics of this bounded mapping, not compatibility with an independent
ReqIF implementation. It requires durable identity to become authoritative,
visible, and exchanged from the start.

### Comparative result

| Concern | ID only | Continuity assertion | Durable identity |
| --- | --- | --- | --- |
| Standalone `.mreq` readability | Best; one identity | Unchanged source, but meaning may depend on companion history | Two identifiers must be understood |
| Atomic internal correction | Sufficient | Sufficient plus assertion | Sufficient plus binding update |
| Snapshot semantics | Replacement | Replacement unless companion changes repository semantics | Same durable object, changed human ID |
| Stale human-ID repair | Manual Git/provenance investigation | Exact when assertion is supplied | Only through a previously shared binding |
| ReqIF transport continuity | No | Possible cooperating adapter policy | Yes when durable ID is transport identity |
| New concepts/policy | None | Assertion authority, chains, split/merge | ID generation, authority, visibility, migration |

## Decision

Retain the human-facing ID as the sole 0.2 requirement identity for now. Treat
an ID change as semantic replacement in each source snapshot, even when a pull
request clearly records a correction. Require internal references to change
atomically.

Do not add aliases, continuity assertions, or durable machine IDs from this one
scenario. Durable identity clearly wins for cross-baseline synchronization only
when a consumer already exchanged that identity. Mundane-req has demonstrated
the mechanics locally, not the need through an independently baselined consumer.

Reopen the decision if any independently baselined consumer shows that update
matching requires pre-exchanged identity beyond the human ID and reviewed Git
changes. At that point prefer an explicit, visible authority over hidden IDs.
Provenance and unchanged content may support a continuity decision but must not
silently define identity.
