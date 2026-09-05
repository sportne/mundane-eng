# Source 1.0 Readiness Audit

Status: Complete

Date: 2026-08-30

Roadmap task: [TC-1001](../roadmap/closed/task-1001-audit-readiness-for-1.0.yaml)

## Decision

mundanereq has enough evidence to proceed to a **source-language-only 1.0
publication decision** in TC-1002, but not to publish immediately.

The recommended 1.0 source contract is a stability release of the language
already identified as `mundanereq-source-0.2`, not a feature release. It
should accept every conforming 0.2 source set and produce exactly the same
semantic interpretation. No grammar or model addition is justified as part of
publication.

This recommendation does not stabilize:

- validator, formatter, or trace command lines;
- diagnostic categories, wording, or machine-readable output;
- formatter policy;
- trace output;
- Java packages or APIs;
- native binary platforms or support policy;
- ReqIF profiles or cross-tool interchange;
- verification, assessment, glossary, allocation-companion, view, or policy
  languages; or
- claims of human usability, certification suitability, or operational
  readiness.

Those surfaces are separate by design. A stable source contract does not
require every tool around it to become stable at the same time.

The audit's first 2026-08-30 execution of `make verify` found a stale
verification boundary: `FormatterVerificationTest` omitted valid source sets
added by Experiments 0016, 0017, 0018, 0020, 0021, and 0022. TC-1002 resolved
that blocker by adding 15 independent source-set selections. Focused formatter
verification now passes over 29 source sets and 60 files, and the complete
GraalVM `make verify` gate passes. No formatter behavior change was required.

TC-1002 must still decide whether the absence of human authoring,
normal-review, and real formal-traceability workflow evidence is acceptable for
a source-compatibility release or requires deferral.

## Audit method

The audit applies every [Roadmap Stage 10](../roadmap/0001-initial-roadmap.md#stage-10--decide-whether-to-stabilize-10)
criterion to committed specifications, fixtures, experiments, decision
records, tool contracts, tests, tags, and distribution evidence. It treats
tool existence as supporting evidence only where the tool exercises a written
source rule or demonstrated workflow.

Ratings mean:

- **Met:** evidence is sufficient for the bounded source-language decision.
- **Met with limitation:** evidence supports a narrow claim while a named risk
  remains.
- **Release blocker:** a bounded defect or missing check must be resolved before
  publication.
- **Release work:** the evidence decision is made, but TC-1002 must create or
  reconcile publication artifacts before 1.0 exists.
- **Not met:** the criterion lacks the evidence it asks for.
- **Deferred:** a real evidence gap remains outside the proposed source 1.0
  promise.

## Stage 10 criteria

| Criterion | Assessment | Evidence and limitation |
| --- | --- | --- |
| Independent users can understand and author from the written standard | **Not met as written; independent-agent proxy only** | [Experiment 0012](../experiments/0012-independent-author-trial/README.md) and [Experiment 0023](../experiments/0023-multi-subagent-author-review/README.md) show fresh-context interpretation, authoring, repair, trace use, and ordinary review without project history. Both use AI agents. The project owner explicitly substituted agents for the planned human case, so they provide useful ambiguity pressure but no human learnability, effort, or editor-workflow evidence. |
| Validator implements the standard without known deviation | **Met** | The [0.2 conformance audit](0010-source-0.2-conformance-audit.md), [maintained migration comparison](0013-maintained-interpreter-migration-closeout.md), and [validator verification](0014-validator-verification.md) cover all conformance selections, twelve maintained interpretation groups, project corpora, source selection, diagnostics, repair, JVM/native agreement, and historical-oracle inventory equality. No known source-validity or semantic deviation remains. |
| Formatter output is semantic-preserving, idempotent, and accepted in normal review | **Preservation and idempotence met for the current corpus; normal-review acceptance not met as written** | [Formatter verification](0015-formatter-verification.md) establishes semantic preservation, authored nonblank-text and comment preservation, opaque-math preservation, byte idempotence, and JVM/native agreement across 29 independently selected source sets and 60 files. [Experiment 0010](../experiments/0010-integrated-toolchain-trial/README.md) provides only a controlled ordinary-diff and simulated branch/review workflow; no independent human normal-review acceptance evidence exists. |
| Trace analysis answers real formal-traceability workflows | **Not met as written; controlled workflow proxy only** | [Research 0016](0016-first-trace-interface.md) derives four bounded questions from sustained-authoring experiments. [Experiment 0009](../experiments/0009-trace-workflows/README.md), [Experiment 0010](../experiments/0010-integrated-toolchain-trial/README.md), and [Experiment 0023](../experiments/0023-multi-subagent-author-review/README.md) exercise direct and transitive navigation, impact paths, cycles, repairs, and agent review. They demonstrate tool behavior against representative formal-traceability questions, not use in a real systems-engineering workflow. |
| A meaningful corpus and multi-author Git history have been maintained | **Met for controlled project evidence; real-team operation is untested** | The original UAS corpus, the licensed 19-requirement [NASA FRET transfer](../experiments/0004-transferability/README.md), the 60-requirement/54-link [operational corpus](../experiments/0011-operational-corpus/README.md), [multi-author layout histories](../experiments/0013-multi-author-layout-trial/README.md), and the two independent Experiment 0023 branches cover addition, change, deletion, movement, retargeting, conflict, merge, baselines, and review. These are bounded constructed experiments, not sustained maintenance by a real engineering team. |
| Language evolution and repository version selection are understood | **Met for one repository-wide contract; mixed versions deferred** | The [0.2 contract](../specification/0006-provisional-0.2-contract.md) records that 0.2 adds only nonsemantic comments and preserves every 0.1 interpretation. Annotated tags `provisional-0.1` and `provisional-0.2` retain both baselines, while the repository [VERSION](../VERSION) selects `0.2-provisional` without adding per-file directives. No real workflow requires incompatible versions in one source set. |
| Identity correction and consequential model pressures have dispositions | **Met** | Identity remains the human ID after a bounded continuity comparison ([Research 0023](0023-identity-continuity-decision.md)). Verification facts are separate companion concepts ([Research 0024](0024-verification-companion-decision.md)); safety levels are contextual assessments ([Research 0025](0025-safety-classification-ownership-decision.md)); allocation remains a label with project policy ([Research 0027](0027-allocation-model-decision.md)); glossary and symbols remain prose/local definitions ([Research 0028](0028-glossary-and-symbol-decision.md)); and trace rules remain project policy ([Research 0029](0029-trace-policy-decision.md)). None requires a 1.0 grammar addition. |
| Conformance supports another implementation without CLI accidents | **Met with bounded-corpus limitation** | [Experiment 0015](../experiments/0015-independent-conformance/README.md) sealed the normative standard and 16 selections away from implementation source and expected output. An independent interpretation agreed on all validity decisions and both complete semantic inventories. Five diagnostic-anchor differences are conforming because exact anchors and CLI text are intentionally nonnormative. |
| Native tools build, version, distribute, and operate independently | **Met for the documented trial platform** | [Research 0017](0017-native-suite-packaging.md) and the [distribution guide](../distribution/README.md) establish three no-fallback GraalVM native executables, independent installation, checksums, notices, a baseline x86-64/glibc-2.34 boundary, and a tagged source-to-package procedure. [Research 0018](0018-clean-checkout-ci-workflow.md) verifies explicit independent CI steps. This is not a multi-platform or production-support promise. |
| The project can state intentional exclusions | **Met by this audit** | The exclusions and deferred evidence below distinguish permanent source-boundary decisions from questions that merely lack evidence. |

## Compatibility surfaces

### Source language

**The semantic compatibility surface is ready; publication remains
conditional.**

TC-1002 should publish `mundanereq-source-1.0` as semantically identical to
`mundanereq-source-0.2`. The compatibility promise should cover:

1. accepted physical source, source selection, lexical and syntactic forms;
2. the requirement and decomposition semantic model;
3. source-set validity;
4. comment omission from semantics;
5. file-layout, order, and line-ending equivalence; and
6. the minimum implementation-conformance requirements stated by the normative
   standard.

Every conforming 0.2 source set should be conforming 1.0 source with the same
semantic value. Clarifications may improve wording without changing accepted
source or interpretation. Any future change that rejects 1.0 source or changes
its semantic interpretation requires a new incompatible contract; additive
syntax requires an explicitly versioned successor.

### Validator CLI

**Not ready for a stable CLI promise and not required for source 1.0.**

The [validator trial contract](../specification/0007-validator-trial-contract-0.1.md)
is maintained and well tested, but diagnostic categories and text remain
provisional and no machine-readable interface exists. TC-1002 may update the
tool's reported source-contract identity only through an explicit tool-trial
revision. It must not silently rename `trial-0.1` as CLI 1.0.

### Formatter CLI and policy

**Not ready for a stable CLI or formatting-policy promise and not required for
source 1.0.**

The [formatter trial contract](../specification/0008-formatter-trial-contract-0.1.md)
has strong safety evidence, but formatting is not source conformance, and
metadata preservation and set-wide transactionality are deliberately bounded.
Its trial version remains independent.

### Trace CLI and output

**Not ready for a stable CLI, graph-query language, or machine-output promise
and not required for source 1.0.**

The [trace trial contract](../specification/0009-trace-trial-contract-0.1.md)
defines useful human-readable behavior. No consumer justifies freezing its text
as a machine protocol.

### Java implementation API

**No public API exists.**

[Research 0011](0011-maintained-implementation-lineage.md) deliberately keeps
packages, classes, records, methods, and module boundaries internal. Source 1.0
must not accidentally create a Java compatibility promise.

### ReqIF interchange

**Experimental and deferred.**

[Experiment 0006](../experiments/0006-reqif-interchange/README.md) proves one
bounded schema-valid semantic self-roundtrip. It explicitly does not prove
independent-tool interoperability, update behavior, arbitrary ReqIF import, or
unknown-attribute preservation. TC-0902 remains conditional on a credible
exchange workflow and independent implementation.

### Native distribution

**Maintained trial distribution, not a portable release-support promise.**

The source-to-package process and exact Linux ABI boundary are documented.
Prebuilt multi-platform availability, bit-identical native builds, support
lifetime, qualification, and certification remain outside the promise.

## Model-pressure disposition

| Pressure | 1.0 disposition | Reopening evidence |
| --- | --- | --- |
| Human-facing ID correction | Retain sole human ID; correction is semantic remove/add with atomic link updates | An independently baselined consumer that needs pre-exchanged continuity |
| Verification planning and evidence | Keep outside requirement records; conceptual companion model selected, carrier provisional | Another real verification workflow plus a focused analyzer |
| Safety or criticality | Keep as contextual, baseline-bound assessment assertion | A workflow that justifies a stable carrier or scheme policy |
| Allocation vocabulary and identity | Retain optional opaque label; allowed values are project policy | Real rename continuity or multi-target responsibility need |
| Glossary and reused terms | Use prose, local definitions, and search | Larger workflow where continuity or impact is ambiguous |
| Formal symbols and LaTeX | Keep LaTeX opaque with adjacent definitions | Concrete analysis or renderer need requiring a bounded profile |
| Trace completeness and cycles | Keep source conformance separate from scoped policy | Repeated policy with explicit scope and waiver semantics |
| Authored views and document order | No 1.0 syntax | Concrete delivery/review workflow requiring authored composition |
| Diagnostic truncation and machine output | Unlimited human text remains current behavior; bounded option selected but unimplemented | Concrete user or CI consumer |

## Publication work required in TC-1002

The project is **not yet published as 1.0**. The following are release work,
not unresolved evidence questions:

1. Write the normative 1.0 standard and contract as a no-feature,
   semantics-identical successor to 0.2.
2. State the exact source compatibility and migration policy.
3. Change repository-level `VERSION` from `0.2-provisional` to the selected
   1.0 identifier and state that one source set uses one externally selected
   contract.
4. Establish a `conformance/1.0` baseline, reusing 0.2 cases where byte-for-
   byte reuse is intentional and recording the relationship explicitly.
5. Decide how maintained trial tools report or claim the new source contract
   without stabilizing their CLIs or Java APIs accidentally.
6. Reconcile README, project/design specifications, roadmap status, distribution
   documents, and trial contracts so none call the source release provisional
   or imply that tool 1.0 accompanies it.
7. Create an annotated source-contract release tag and verify the complete
   clean-checkout evidence from that exact commit. The stale formatter
   maintained-source-set inventory discovered by this audit has been repaired
   and `make verify` passes; the gate must run again on the eventual release
   commit.
8. Record an explicit publication decision about the unmet human-authoring,
   normal-review, and real-workflow criteria. Either limit 1.0 to a
   compatibility promise while publishing those evidence gaps, or defer 1.0
   and create bounded trials that can satisfy them.

Until TC-1002 completes these actions, `mundanereq-source-0.2` remains the
current provisional contract.

## Evidence gaps and classification

### Publication-decision and post-1.0 evidence risks

These are real unknowns, not rewritten as permanent non-goals:

- **Human learnability and effort.** No independent human systems engineer has
  completed the frozen trial. The agent evidence exposes no source-rule
  ambiguity, but it does not satisfy the human criterion. TC-1002 must decide
  whether that gap blocks publication or is explicitly accepted for a narrow
  compatibility promise. In either case, it blocks claims that 1.0 is broadly
  usable, easy to learn, or validated in ordinary human editor and forge work.
- **Sustained real-team operation.** The corpora and histories are meaningful
  but bounded. Formatter acceptance in normal human review and trace use in a
  real formal-traceability workflow are untested. TC-1002 must decide whether
  these gaps block publication or are explicitly accepted for a narrow
  compatibility promise. Revisit performance, organization, policy, and
  documentation after sustained use on a materially larger real project.
- **Incompatible-version coexistence.** Repository-level selection is
  sufficient while one source set uses one contract. Design mixed-version
  behavior only when a real migration requires it.
- **Independent ReqIF interoperability.** Keep on the future-work roadmap; do
  not claim it in 1.0.
- **Stable companion carriers.** Verification and assessment ownership is
  understood, but their carriers are not source 1.0.
- **Platform breadth and operational support.** Current native evidence is one
  Linux ABI target and supplies no support or qualification claim.

### Conditional experiments not required before source 1.0

- TC-0807 authored views requires a concrete composition workflow.
- TC-0902 independent ReqIF roundtrip requires an independent implementation
  and credible exchange workflow.
- TC-0903 derived presentation requires the TC-0807 disposition and a concrete
  delivery/review need.
- TC-0904 remains locked because no additional ecosystem tool has been
  selected.

Running these merely to accumulate features would not strengthen the source
syntax and semantic compatibility evidence.

## Intentional source 1.0 exclusions

The source 1.0 contract should explicitly exclude:

- document hierarchy, authored views, ordering, and rendering;
- arbitrary attributes, configurable schemas, and generalized metamodels;
- approval, signatures, review workflow, access control, and baseline authority;
- verification plans, executions, evidence, results, and satisfaction semantics;
- safety-classification or other assessment carriers and scheme vocabularies;
- component models, controlled allocation vocabularies, and multi-target roles;
- glossary, macro, formal-symbol, or executable-mathematics semantics;
- trace completeness, acyclicity, prose quality, and project-policy validity;
- variants, reuse mechanisms, cross-repository links, and package management;
- persistent indexes, databases, servers, and custom forge/review systems;
- semantic diff and semantic merge;
- ReqIF or other interchange behavior;
- stable diagnostic, formatter, trace, Java API, or machine-output interfaces;
- prebuilt platform coverage, support lifecycle, certification, qualification,
  or regulatory claims; and
- any claim that source conformance means a requirement is correct, complete,
  approved, safe, allocated validly, or verified.

These exclusions define a small compatibility surface. They do not prohibit
independent tools or later separately versioned companion standards.

## Recommendation to TC-1002

Proceed to TC-1002 with a **conditional publication recommendation** for a
source-language-only 1.0 release. The complete verification condition is now
met. Publication still requires an explicit decision about the unmet human-
authoring criterion and absent normal-review and real-workflow evidence. If
that evidence is treated as mandatory for 1.0, defer and create the bounded
trials instead.

TC-1002 should not add features to make the release look more complete. It
should convert the demonstrated 0.2 language into a deliberate long-term
compatibility promise, keep every adjacent interface separately versioned, and
publish the residual evidence risks without disguising them as solved.
