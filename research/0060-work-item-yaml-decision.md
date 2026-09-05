# Research 0060: YAML work-item source decision

TC-1607. Decision recorded 2026-09-05.

Select YAML 1.2 for work items independently of requirements. A complete record is
one mapping: format, id, kind, title, status, dependencies, relations, planning and
body. Literal block strings keep readable Markdown and preformatted text in body.
The decoded string is opaque to linking and scheduling. No headings within body
create attributes, acceptance records or relationships. Separate structured narrative
sections can be investigated only with a concrete consumer; they are not part of
this carrier migration.

Research 0055 overvalued keeping existing Markdown files and avoiding a library.
Its claim that YAML requires escaping or restructuring narrative was too broad:
literal block scalars preserve narrative as a single readable string. The YAML
library can be shared without sharing domain models. Markdown remains appropriate
inside body and for generated views, but does not own the record's identity.

[Specification 0019](../specification/0019-work-items-yaml-0.2.md) and its schema
select the profile. One item per file preserves the repository's review and closing
workflow. Multiple items per file are deferred; no demonstrated workflow needs them
in this change. Required semantic facts remain explicit. Empty dependencies,
relations and planning annotations can be omitted; normalization is documented.
Plain scalars are allowed only when YAML Core resolves them to the expected string.
Block scalars are strings without extra tags. Literal style is recommended for body;
folded and quoted strings retain normal YAML semantics rather than custom behavior.

Source selection uses a new explicit manifest version and source field, avoiding
extension guessing or parser fallback. Existing selection 0.1 and Markdown source
remain supported. New compiled output 0.2 retains all semantic values but permits
real YAML locations; consumers also validate old 0.1 with its original invariants.
Analysis stays 0.1: its graph meanings and envelope are unchanged. The requirements
and plan languages and version declarations are untouched.

The structural JSON Schema validates the decoded YAML data model. Duplicate keys,
anchors/tags, document count, physical encoding, limits and source marks require
compiler checks. Graph resolution/cycles require analysis. A generic schema pass
alone cannot establish a valid linked backlog. No default statuses, generated IDs,
automatic closure or evidence approval are added.

Migration must compare compiled semantic values, preserve exact decoded body
strings and inventory every path change. Relative prose links need explicit
retargeting to renamed YAML files; this is recorded separately from source encoding.
Use quoted escapes if a string cannot be represented exactly as a literal (such as
embedded CR characters). Existing completion evidence is retained; only new cards
receive new completion records. Generated indices remain disposable.

Acceptance: worked profile examples, invalid-case table, explicit source/output
compatibility and a checked structural schema are present. TC-1608 provides runtime
conformance evidence; this design does not claim an implemented compiler.

YAML semantics reference: [YAML 1.2.2, block scalars](https://yaml.org/spec/1.2.2/#81-block-scalar-styles).
