# Specification Index

This directory separates normative source-language standards, provisional
contract and compatibility policy, nonnormative design rationale, and
separately versioned tool interfaces.

The default source contract is `mundanereq-source-0.2`; explicit YAML requirements
use `mundanereq-yaml-0.3`. Each has its own normative specification and selection
rules. Both remain experimental. The [current roadmap](../roadmap/0001-initial-roadmap.md) plans
incremental monorepo, compilation, linking, and tooling work. Those cards do not
change normative syntax or existing CLI contracts; adopted changes require an
explicit contract update and applicable compatibility notes.

## Recommended reading order

1. [Project foundation](0001-project-foundation.md) for mission, scope, and
   product philosophy.
2. [Minimum source language and model](0002-minimum-source-language-and-model.md)
   for design rationale and model-pressure dispositions.
3. [Source Language Specification 0.2](0005-mundanereq-source-language-0.2.md)
   for normative language behavior.
4. [Provisional 0.2 Contract](0006-provisional-0.2-contract.md) for version,
   compatibility, exclusions, and trial expectations.
5. The relevant tool contract for a maintained command-line interface.

## Documents

| Document | Role | Current status |
| --- | --- | --- |
| [0001 — Project Foundation](0001-project-foundation.md) | Mission, scope, principles, and current dispositions | Living, nonnormative |
| [0002 — Minimum Source Language and Model](0002-minimum-source-language-and-model.md) | Design record and rationale for the current model | Current, nonnormative |
| [0003 — Provisional 0.1 Contract](0003-provisional-0.1-contract.md) | Prior source contract and compatibility policy | Historical provisional contract |
| [0004 — Source Language Specification 0.1](0004-mundanereq-source-language-0.1.md) | Complete normative 0.1 language | Historical provisional standard |
| [0005 — Source Language Specification 0.2](0005-mundanereq-source-language-0.2.md) | Complete normative current language | Current provisional standard |
| [0006 — Provisional 0.2 Contract](0006-provisional-0.2-contract.md) | Current source contract, compatibility policy, and exclusions | Current provisional contract |
| [0007 — Validator Trial Contract 0.1](0007-validator-trial-contract-0.1.md) | `mundanereq-validate` CLI behavior | Maintained trial; not stable CLI |
| [0008 — Formatter Trial Contract 0.1](0008-formatter-trial-contract-0.1.md) | `mundanereq-format` CLI and policy | Maintained trial; not stable CLI or policy |
| [0009 — Trace Trial Contract 0.1](0009-trace-trial-contract-0.1.md) | `mundanereq-trace` CLI and output | Maintained trial; not stable CLI or output protocol |

| [0010 — Requirements YAML 0.3](0010-requirements-yaml-0.3.md) | YAML mapping, schema and requirement semantics | Normative experimental contract |
| [0011 — Tool safety and YAML commands](0011-tool-safety-and-yaml-commands.md) | Explicit selectors, output/snapshot safety and migration | Normative additive command contract |

## Authority boundaries

- Specification 0010 and its linked structural schema control YAML 0.3; 0011
  defines current command additions and safety behavior.
- Specification 0005 controls 0.2 syntax, semantics, validity, conformance,
  and semantic equivalence.
- Specification 0006 controls the provisional 0.2 compatibility promise and
  repository-level contract selection.
- Specifications 0001 and 0002 explain why the project made its decisions; they
  do not override normative language rules.
- Specifications 0007 through 0009 define independent tool trials. Conforming
  source does not depend on those CLIs, their diagnostic text, the Java
  implementation, or GraalVM.
- Research and experiments are evidence and decisions. They change a contract
  only when the applicable specification is updated explicitly.

- [Requirement semantic output 0.1](0012-requirement-semantic-output-0.1.md)
- [Compiled diagnostic rule catalog](0013-compiled-diagnostic-rules.md)

- [Local artifact imports and linking 0.1](0014-local-artifact-imports-0.1.md)

- [Verification planning 0.1](0015-verification-planning-0.1.md)

- [Diagnostic recovery and incomplete interpretation](0016-diagnostic-recovery.md)

- [SARIF validation output](0017-sarif-validation-output.md)

- [Work items 0.1](0018-work-items-0.1.md)

- [YAML work items 0.2](0019-work-items-yaml-0.2.md)

- [Project attributes and YAML 0.4](0020-project-attributes-yaml-0.4.md)
