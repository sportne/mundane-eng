# Research 0054: Work-item ownership

Date: 2026-09-05. Decision for TC-1601.

Select two work-item kinds. A task describes intended work and an authored completion
claim; an issue describes an observed/reported problem and an authored closure claim.
Neither closing an issue nor completing an associated task proves a requirement is
satisfied. A defect can remain open after an investigative task completes.

The input inventory at commit 10a7437 contains 72 cards: 62 Complete, one qualified
Complete, five Planned, one Ready and three Conditional. All have human IDs, titles,
stage/type/prerequisite/unlocks metadata and prose. Dependency wording sometimes
combines IDs with conditions; one historical completion status has a qualification.
Preserve those qualifications, not just normalized status values. Six execution
cards for this batch bring the selected corpus to 78.

| Fact | Authority | Mechanized meaning |
| --- | --- | --- |
| ID and kind | Work-item author, source card | Identity within selected set; task/issue are distinct kinds |
| Title and prose | Card author | Description, scope and recorded evidence; no inferred truth |
| Task status | Card author | Ready, Planned, Conditional, In progress, Complete or Superseded |
| Issue status | Issue author | Open, Closed or Superseded; closure is not automatic |
| Dependencies | Dependent task author | Local task prerequisites; referenced task must exist; cycles invalid |
| Condition/qualifications | Card author | Visible policy, not executable assertions; dependency completion alone does not authorize execution |
| Addresses / relates-to | Linking card author | Target exists with stated kind/scope; no completion/satisfaction consequence |
| Evidence | Card author | A cited local resource exists, not proof its contents support the claim |
| Supersedes | New card author | Explicit link to an older work item; no status mutation or inherited dependencies |
| Reverse links / unfinished prerequisites | Derived analysis | Preserve originating card and relation location; no editable inverse inventory |

Keep issue dependencies empty initially: scheduling belongs to tasks. A task may
address an issue; completion of the task leaves the issue's status unchanged.
Superseded dependencies remain unfinished until an author revises the dependency;
do not silently substitute a successor. A Superseded card must have an incoming
supersedes relation, without implying every supersedes target must already be retired.
Supersession cycles/self-links are invalid independently of dependency cycles.

Worked example: ISSUE-1 reports an absent alarm; TC-FIX depends on TC-INVESTIGATE
and addresses ISSUE-1 plus requirements:SYS-001. TC-INVESTIGATE completes with a
finding, while TC-FIX remains Planned and ISSUE-1 Open. The derived analysis reports
no unfinished prerequisites for TC-FIX, but does not rewrite it to Ready. A cited
source-code path is a resource link, not a compiled code entity or tested behavior.
A plan/activity target has its own typed compiled reference. Unknown kinds fail;
future artifact adapters must define their own target validation.

Select a bounded first relationship vocabulary: addresses and relates-to target
work items, requirements, verification plans or activities; evidence targets local
resources; supersedes targets work items. No arbitrary relation schema, remote
tracker synchronization, workflow engine, dates, assignees or generated IDs are
required. Prose can retain ordinary links without claiming typed resolution.

TC-1602 must select notation without duplicating status/dependency authority.
Compare the existing Markdown header with structured metadata, preserving narrative
bytes. Requirements YAML does not decide work-item representation. Source moves
change provenance, not identity. The compiler/linker consume snapshots; Git retains
historical revisions. This is repository inspection and worked reasoning, not user
feedback, adoption evidence or a hazard/verification judgment.
