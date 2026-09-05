# Research 0012: Shared Source Representation

Status: Decided and prototyped

Decision date: 2026-08-23

Roadmap task: [TC-0202](../roadmap/closed/task-0202-design-the-shared-source-representation.yaml)

## Question

What source information must be retained so validation, conservative
formatting, and semantic tools can share one interpretation without making
formatting trivia part of a requirement?

## Decision

Retain one ordered `ConcreteLine` for every physical line. Each line contains:

- exact decoded line text excluding its terminator;
- its original LF or CRLF spelling;
- a half-open, Unicode-scalar-counted source span; and
- one lexical kind: blank, full-line comment, or other content.

`SourceDocument` also retains the original UTF-8 bytes and can reproduce them
exactly. The prototype accepts only decodable, terminated physical input;
TC-0203 will route physical failures through maintained diagnostics rather
than exceptions.

## Semantic boundary

Blank lines and comments remain concrete source. They do not enter the
requirement model. A comment has no owner, attachment, or inferred association
with a record or field. `CONTENT` is intentionally not subdivided into a
general token tree: TC-0203 will interpret the small line-oriented grammar
directly from the ordered lines.

The comment-free and commented conformance fixtures have equal sequences of
potentially semantic content lines. Maintained semantic interpretation now
passes each losslessly reproduced `SourceDocument` through the same parser;
both produce the same normative semantic inventory. The concrete prototype also
reproduces the commented source exactly while exposing its twelve comments
separately. The inventory text is evidence, not an interchange API.

## Consumers

| Retained information | Identified consumer |
| --- | --- |
| Original bytes | preservation checks and conservative formatter safety |
| Ordered physical lines | parser and formatter |
| Exact line text | parser, diagnostics, and formatter |
| LF/CRLF spelling | formatter experiment |
| Unicode-scalar source span | diagnostics |
| Blank/comment/content kind | parser trivia handling and comment-preserving formatter |

Trace analysis consumes only the later semantic model and relationship index;
it has no reason to depend on concrete lines.

## Rejected additions

The representation does not include tokens, a generalized syntax tree,
comment attachment, mutable nodes, parent pointers, path-derived modules,
semantic whitespace, formatter directives, or LaTeX parsing. Math payload
lines remain exact ordinary physical-line text until the grammar identifies
their structural indentation and bounds.

## Decision gate

Proceed to maintained semantic parsing. If the first conservative formatter
cannot preserve comments and opaque payloads from these ordered lines and
small grammar nodes, narrow formatter behavior before introducing a general
lossless-syntax framework.
