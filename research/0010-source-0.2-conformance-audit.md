# Research 0010: Source 0.2 Conformance Baseline Audit

Status: Completed

Result date: 2026-08-23

Roadmap task: [TC-0103](../roadmap/closed/task-0103-audit-the-0.2-conformance-baseline.yaml)

## Question

Does the maintained conformance material cover `mundanereq-source-0.2` well
enough to protect extraction of shared implementation code without turning
reference-probe accidents into language requirements?

## Conclusion

Yes, for the planned extraction. The normative valid inventories, focused
invalid fixtures, and 14 grouped probe tests cover every normative rule area
or provide enough adjacent evidence to detect a semantic regression. No open
ambiguity was found that permits two conforming implementations to assign
different semantic values to the same conforming source.

This is not an assertion that the fixture set is exhaustive. Several useful
additional cases are identified below. They do not block refactoring because
the relevant rules are explicit, existing behavior is covered by a
representative case, and TC-0204 requires comparison with the historical
probe and all maintained corpora.

## Evidence layers

The baseline has three different evidence layers:

1. **Normative examples.** The valid 0.2 source and expected inventory under
   `conformance/0.2/valid` are normative examples through the provisional 0.2
   contract. They constrain semantic interpretation, not original formatting
   reproduction or inventory syntax generally.
2. **Representative conformance inputs.** Invalid fixtures in `conformance/0.1`
   cover the grammar and model inherited unchanged by 0.2. The 0.2 invalid
   fixtures add comment-placement and Unicode edge cases. They supplement the
   language standard rather than replacing it.
3. **Reference implementation tests.** `ProbeTest` exercises combinations,
   filesystem behavior, semantic equivalence, diagnostics, and command
   behavior. These tests protect implementation behavior during extraction,
   but their CLI strings and Java interfaces are not thereby normative.

The ReqIF probe is adjacent-consumer evidence. It confirms that source
comments do not enter the semantic model or derived ReqIF. ReqIF behavior is
outside the source-language contract.

## Normative coverage map

| Rule area | Evidence | Audit disposition |
| --- | --- | --- |
| Conforming file, set, and interpreter (Clauses 2 and 10) | Valid 0.1/0.2 fixtures; all invalid fixtures; `commandInterface` | Covered. The probe's exact process interface is not normative. |
| Semantic requirement members and optional absence (Clause 5.2) | Valid inventory; `optionalFields`; `equivalentLayouts` | Covered, including absence distinct from an empty value. |
| Identity independent of location and order (Clauses 5.3 and 13) | Module/one-file inventory equality; file-move history in Experiment 0003 | Covered semantically. Cross-baseline ID continuity is deliberately outside 0.2. |
| Exact scalar semantics (Clause 5.4) | Valid fixture source values; `sourceComments`; candidate corpora | Covered for punctuation and preserved text. No quoting, escaping, or trimming is performed. |
| Directed decomposition semantics (Clause 5.5) | Valid inventory; relationship tests; incoming-query test | Covered. Incoming traces are derived and outside language conformance. |
| Ordered prose and math content (Clauses 5.6 and 9) | Valid inventory; `proseAndMathInterpretation`; UAS math fixture | Covered for folding, paragraph boundaries, prose/math order, de-indentation, and opaque payload preservation. |
| UTF-8, BOM, line endings, controls, final EOL (Clause 6) | `physicalSourceDiagnostics`; `lineEndingEquivalence`; four Unicode fixtures | Covered. Malformed UTF-8 raw-byte positioning, C1 controls, and supplementary-scalar positioning have exact-coordinate checks. |
| Explicit source selection (Clause 7.1) | `explicitFileDiscovery`; missing-input command test | Covered for explicit files regardless of extension, directories, unavailable input, and empty selection. |
| Recursive selection and `.git`/symlink handling (Clause 7.2) | `sourceDiscovery` | Covered for recursive `.mreq` selection, descendant `.git` exclusion, symlink exclusion, ignored non-`.mreq` files, and deduplication. |
| File granularity (Clause 7.4) | Candidate A/B equal inventories; multi-record conformance source | Covered directly. |
| Comment syntax, placement, and semantic omission (Clause 8.1.1) | 0.2 valid fixture; four comment-invalid fixtures; `sourceComments` | Covered at every permitted boundary and prohibited prose/math locations. |
| Identifier and scalar syntax (Clauses 8.2 and 8.3) | `recordAndFieldDiagnostics`; Unicode boundary fixtures; relationship tests | Covered representatively. Exact ID regex and explicit whitespace set are implemented without runtime predicates. |
| Record form, cardinality, order, and separation (Clauses 8.4 and 8.5) | 0.1 invalid fixtures; `recordAndFieldDiagnostics`; commented 0.2 source | Covered for missing, duplicate, unknown, out-of-order, nested, unclosed, adjacent, and comment-separated records. |
| Prose body form and folding (Clauses 8.6 and 9.2) | Valid inventory; `proseAndMathInterpretation`; body diagnostics | Covered for indentation, nonemptiness, wrapping, blank-line paragraph separation, and preservation after structural indentation. |
| Statement math blocks (Clause 9.3) | Valid inventory; exact UAS payload; `mathDiagnostics`; comment/math invalid fixture | Covered for recognition, rationale exclusion, payload indentation, emptiness, unexpected end, and untermination. |
| Duplicate targets, global IDs, and dangling references (Clauses 10 and 11) | 0.1 invalid fixtures; `identityAndRelationshipDiagnostics` | Covered across records and files. Cycles remain valid source by design. |
| Policy is not language validity (Clause 11.3) | Specification exclusions; corpora with optional source/relationships; positive self- and multi-record-cycle test | Covered by boundary rather than universal policy tests. Future checks must identify themselves as policy. |
| Diagnostic information and coverage (Clause 12) | Every negative test checks file, positive line/column, category, and message; selected exact-coordinate assertions | Covered. Exact codes, wording, ordering, and recovery remain nonnormative. |
| Semantic equivalence (Clause 13) | File-layout equality, LF/CRLF equality, comment/no-comment equality, sorted target inventory | Covered for the transformations used by planned extraction. |
| Revisions, baselines, contract ID, and compatibility (Clauses 14 and 15) | Specifications 0005/0006; `VERSION`; annotated `provisional-0.1` and `provisional-0.2` tags | Covered as environment and documentation behavior, not parser fields. |
| Excluded facilities (Clause 16) | Normative exclusion list; semantic inventory shape | Covered as absence. No fixture should imply that a deferred facility is recognized. |

## Comment omission across consumers

The source-comment test compares the 0.1 valid inventory with the commented
0.2 inventory and requires byte-identical normalized output. The ReqIF
conformance roundtrip repeats that comparison, performs a semantic roundtrip
of the commented model, and verifies that exported XML does not contain the
fixture's author-comment text.

Together these checks show that comments are retained source syntax but are
absent from the requirement model and from an adjacent semantic consumer.
They do not promise comment preservation by a future formatter; that requires
a concrete-source representation under TC-0202 and formatter-specific
evidence.

## Deliberate coverage gaps

### Required while extracting maintained code

TC-0204 must port or compare all of the following before experiment code is
retired or bypassed:

- both normative valid inventories;
- all 0.1 and 0.2 invalid fixtures;
- all 14 grouped Experiment 0002 tests;
- comment omission in the ReqIF conformance roundtrip;
- equal inventories for module and one-record-per-file corpora.

This is preservation work, not a request for more language features.

### Useful additional cases

The following would strengthen an eventual independently maintained
conformance suite but do not block extraction:

- one table-driven test covering every enumerated scalar-boundary whitespace
  code point rather than the representative U+0020 and U+00A0 cases;
- explicit traversal of a directory whose root itself is named `.git`, as
  distinct from the already tested descendant exclusion;
- uppercase `.MREQ` nonselection to demonstrate exact suffix case;
- leading and trailing blank file trivia in combination with comments;
- reordered requirement records and reordered decomposition lines compared
  directly for semantic equality;
- U+FEFF away from the beginning of a file as ordinary content;
- duplicate byte-identical records called out as a dedicated fixture rather
  than only covered by the general duplicate-ID rule.

These cases are useful for cross-implementation confidence. None exposes an
unresolved semantic rule in the written standard.

### Intentionally outside conformance

No source-language fixture is needed for prose quality, decomposition
completeness, cycle policy, allocation vocabulary, approval, verification
coverage, baseline authority, rendering, ReqIF, or formatter behavior. Tests
for those concerns must identify a separate policy, tool, or companion model.

## Reference-probe behavior that is not normative

The following current behavior must not be frozen accidentally during shared
code extraction:

- the executable name `mundanereq`;
- the `--inventory` and `--incoming` option spelling;
- summary, trace, and usage text;
- exact exit-status assignments;
- diagnostic code strings, wording, punctuation, ordering, and stop-after-first
  recovery behavior;
- normalized-inventory serialization and sorting format;
- Java package names, records, method visibility, and class structure;
- the native binary filename and build-directory layout.

The 0.2 contract requires understandable source-positioned diagnostics and
enough semantic output to compare the normative inventory. It explicitly
does not standardize these reference interfaces. Future tool cards may define
their own trial contracts.

## Baseline reproducibility

The `provisional-0.1` and `provisional-0.2` annotated tags continue to identify
their original contracts and fixtures. This audit and the Unicode repairs are
later implementation evidence; they do not move those tags or rewrite the
historical source-language baselines.

## Decision

The 0.2 conformance evidence is sufficient to begin a maintained
implementation lineage and shared-code extraction. There is no unresolved
semantic ambiguity requiring a source-language revision. Preserve the
evidence layers and nonnormative CLI boundary stated here when executing
TC-0104 through TC-0204.
