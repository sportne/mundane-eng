# Research 0051: Project attribute use-case decision

Date: 2026-09-05. Task: [TC-1301](../roadmap/closed/task-1301-classify-project-attribute-use-cases.yaml).

Status: Selected design scope; attributes are not implemented.

## Decision and evidence boundary

Select project-declared **text and enumeration** attributes for descriptive facts
owned and revised with a requirement. Permit optional or required single values.
Do not supply defaults, repeat keys, infer values, or give an annotation assessment
authority. A required attribute is project source validity, not a universal field
of every requirement. Human-authored requirement IDs remain the only requirement
identity.

This is a worked design assessment against checked-in evidence, not a usability
trial or proof of demand for every example. The user-requested project metadata
capability supplies the immediate design need. The
[ownership matrix](0037-requirement-and-assertion-ownership.md),
[safety experiment](../experiments/0018-safety-classification/README.md),
[allocation experiment](../experiments/0020-allocation-model/README.md), and
[verification report](0046-verification-report-decision.md) constrain its scope.
The report is a concrete derived consumer to extend for displaying annotations;
its current implementation does not yet display custom attributes or route reviews.

The [device requirements](../examples/yaml/vaccine-monitoring/device.mreq.yaml)
already use authored `source` citations, including multiple clauses in one string,
and `allocation` labels for product responsibilities. Preserve these meanings.
Do not introduce a second copy of those fields merely to exercise custom metadata.

## Use-case and ownership matrix

“0..1 / 1” means optional or required as explicitly declared by a project; each
present value occurs once. Examples below are illustrative project vocabularies,
not safety standards, established project practices or contributor feedback.

| Candidate | Owner and authoritative source | Type/cardinality disposition | Context and concrete consumer | Decision |
| --- | --- | --- | --- | --- |
| `discipline`: software / electronics / mechanical | Requirement author, requirement source under checked-in project vocabulary | Enumeration, 0..1 / 1 | Descriptive classification of this requirement revision; show in the existing review report | Include; no implied allocation or reviewer approval |
| `owner-team`: Logger firmware | Requirement author, requirement source | Text, 0..1 / 1 | Descriptive contact label for review handoff, distinct from product `allocation`; show beside the requirement in that report | Include as a worked text example; does not authenticate membership or authority |
| `safety-topic`: temperature / power / communications | Requirement author, requirement source | Enumeration, 0..1 / 1 | Descriptive topic in review; values say nothing about risk acceptability | Include as ordinary classification, no safety-specific language feature |
| Assessed `safety-criticality`: high / medium / low | Assessor, independently revised assessment artifact | Contextual enum inside that artifact; requirement attributes rejected for this meaning | Scheme, deployment, basis, evidence and authority; two contexts can disagree on an unchanged requirement | Keep outside the requirement |
| Allocation to product version or variant | Allocation author, independent allocation assertion | Domain-specific reference/cardinality decisions deferred to that artifact | Simultaneous variant assignments and changing product structures | Keep outside; built-in opaque allocation remains valid |
| Authored external citation | Requirement author, existing `source` field | Existing optional text | Trace provenance and review source citation | Reuse built-in source; no new `references` synonym needed for this case |
| `references`: several independently addressable external citations | Requirement author for descriptive citations; evidence custodian for evidence claims | List of text deferred; typed URL/reference deferred | Existing pilot cites several clauses, but no current consumer resolves or checks individual citations | Retain opaque citation now; revisit on a consumer needing individual entries, not comma splitting |
| Verification method/status/result | Plan author / executor / result authority, respective companion artifact | Not a requirement enum asserting execution or success | Maintained plan/analyzer already owns planned coverage; execution and acceptance can change independently | Keep outside; a display label cannot establish coverage or satisfaction |
| Regulatory applicability / lifecycle approval | Policy or approval authority, scoped assertion | Contextual rules/claims outside requirements | Jurisdiction, variant, baseline and approving authority can differ independently | Keep outside when consequential; a descriptive topic tag may use enum |
| Engineering requirement reference | Requirement author for decomposition; other relationship's author otherwise | Existing `decomposes`, or independently justified companion relation | Current trace and plan tools mechanize known relationships | Do not hide new edges in text attributes |

Names cannot prove ownership. A project could name an annotation `safety-criticality`;
the parser can check an enum, but cannot establish that it is an assessment. Schema
descriptions must state annotation meaning, and documentation must distinguish a
label from an authoritative claim. Do not pretend that a forbidden-name list could
replace this ownership boundary.

## Minimal type and presence decisions

| Candidate behavior | Decision | Reason / reopening condition |
| --- | --- | --- |
| Text | Include | Human-readable project labels not duplicated by built-in fields; nonempty single-line decoded Unicode, no boundary whitespace, no implicit coercion |
| Enumeration | Include | Exact project-authored permitted strings catch vocabulary mistakes; nonempty unique allowed values, no case folding or normalization |
| Lists / repeated values | Defer | Individual external citations are plausible, but no current consuming operation needs collection semantics; add only with explicit order/duplicate/empty rules and a worked consumer |
| Boolean | Defer | Often compresses a contextual assessment into an unexplained flag; enumerations cover bounded descriptive alternatives |
| Integer / number / units | Defer | No selected descriptive consumer needs arithmetic, range or dimensional validation |
| URL | Defer | Text can preserve a citation without network access; URL validation/resolution is not implied |
| Requirement reference | Defer as an attribute type | References create dependency semantics and need an explicit relationship contract; existing decomposition and companion links cover current cases |
| Optional | Include | Omission means absent, not an empty string or null |
| Required | Include, schema-wide | Applies to every requirement in a schema-selected source set; no conditional requiredness, inheritance or record overrides |
| Repetition / defaults | Reject in this iteration | Duplicate keys are errors; defaults conceal source facts and make a schema edit synthesize changed requirement values |

Text uses the existing YAML title/scalar character rules. It is not a rich-text
paragraph, mini-language, CSV list or hidden link. An enum has the same lexical
value rules plus exact membership. Optional omission is distinct from an enum
member literally called `unknown`; the latter exists only if explicitly declared.

## Worked valid and invalid conceptual values

These are decision examples, not currently accepted requirement syntax. The first
schema example declares required enum `discipline` with `software`, `electronics`,
`mechanical`, and optional text `owner-team`.

| Conceptual assignment | Expected result and reason |
| --- | --- |
| discipline = software; owner-team = Logger firmware | Valid descriptive enum and text |
| discipline = electronics; owner-team absent | Valid; optional omission is not a generated value |
| discipline absent | Invalid: missing required value |
| discipline = Software | Invalid: exact enum membership, no case repair |
| discipline = thermal | Invalid: undeclared enum member |
| owner-team = empty string / null / numeric 7 | Invalid: nonempty text, no null or coercion |
| owner-team = leading-space ` Logger firmware` | Invalid: padded scalar |
| owner-team = two decoded lines | Invalid: text is single-line |
| discipline occurs twice, even with the same value | Invalid: duplicate attribute |
| unknown attribute priority = high | Invalid: project has not declared that attribute |
| references = two-item collection | Unsupported initial type; do not flatten it silently |

Change example: changing only `owner-team` is a requirement annotation revision,
not a new ID. It must survive compilation and be visible to the selected comparison
policy. Changing AS-01's assessed level because hazard evidence changed must instead
revise AS-01 while the referenced requirement can remain unchanged. The two-context
example in Research 0037 remains representable without conflicting attribute values.

## Decision enabled, alternatives and stop conditions

TC-1302 can now select schema storage, YAML attachment, explicit configuration,
source/output evolution, and diagnostic/formatter behavior for exactly these two
types and cardinalities. It must demonstrate no-schema and isolated-file behavior,
and distinguish a schema meaning change from a mere source-location change.

Rejected: a universal metadata framework; making every scalar its own entity;
turning enum labels into safety judgments; copying external relationship inventories
onto requirements; implicit defaults; typing existing allocation as a component ID;
promising list/reference behavior without a consuming operation.

Stop and narrow any design that needs contextual approval rules, inheritance,
network lookup, automatic IDs, or global schema discovery to make this first set
useful. If the report needs only existing source/allocation values, do not invent
additional mandatory attributes for the checked-in pilot. Later implementation must
use a separate realistic example rather than rewrite historical evidence to imply
that the pilot already depended on these annotations.

## Review evidence

Reviewed the selected examples against the cited ownership and current source/output
contracts. Every candidate above has an ownership/type/cardinality disposition;
text and enum each have valid and invalid examples. No implementation tests, user
sessions, interoperability trials, schema files or production changes were made by
this card. Its artifact is this decision record, enabling TC-1302.
