# Specification index

Use the contract for the explicitly selected source or output version. All current
profiles are experimental. The default requirement source remains custom 0.2; YAML
0.3 and attribute-aware YAML 0.4 are opt-in. Work items use their own YAML profile.

## Reading order

Start with the [project foundation](0001-project-foundation.md), then the source
profile for your artifact and the applicable command/compiled-output contracts.
Later addenda extend named base contracts; older documents retain their versioned
scope. Structural schemas cover decoded shape; semantic and physical-source rules
also apply. Research and experiments supply rationale, not overriding syntax.

## Contracts and rationale

| Document | Scope |
| --- | --- |
| [Specification 0001: Project Foundation](0001-project-foundation.md) | Living foundation; nonnormative |
| [Specification 0002: Minimum Source Language and Model](0002-minimum-source-language-and-model.md) | Historical custom-source rationale |
| [Specification 0003: Provisional 0.1 Contract](0003-provisional-0.1-contract.md) | Historical 0.1 compatibility |
| [mundanereq Source Language Specification](0004-mundanereq-source-language-0.1.md) | Historical custom 0.1 syntax |
| [mundanereq Source Language Specification](0005-mundanereq-source-language-0.2.md) | Supported default custom 0.2 syntax |
| [Specification 0006: Provisional 0.2 Contract](0006-provisional-0.2-contract.md) | Custom 0.2 compatibility |
| [mundanereq Validator Trial Contract 0.1](0007-validator-trial-contract-0.1.md) | Validator base contract |
| [mundanereq Formatter Trial Contract 0.1](0008-formatter-trial-contract-0.1.md) | Formatter base contract |
| [mundanereq Trace Trial Contract 0.1](0009-trace-trial-contract-0.1.md) | Trace base contract |
| [Requirements YAML Source 0.3](0010-requirements-yaml-0.3.md) | YAML 0.3 source and structural schema |
| [Tool safety and YAML command addendum](0011-tool-safety-and-yaml-commands.md) | YAML selection, safety and migration |
| [Requirement semantic output 0.1](0012-requirement-semantic-output-0.1.md) | Requirement output 0.1 |
| [Compiled diagnostic rule catalog](0013-compiled-diagnostic-rules.md) | Diagnostic rule catalog |
| [Local artifact imports and linking 0.1](0014-local-artifact-imports-0.1.md) | Imports and linked output 0.1 |
| [Verification planning and review analysis 0.1](0015-verification-planning-0.1.md) | Plan source/output and verification output 0.1 |
| [Diagnostic recovery and incomplete interpretation](0016-diagnostic-recovery.md) | Recovery and incomplete interpretation |
| [SARIF validation output](0017-sarif-validation-output.md) | SARIF output |
| [Work-item source, compilation and analysis 0.1](0018-work-items-0.1.md) | Historical work-item Markdown source/output |
| [YAML work items 0.2](0019-work-items-yaml-0.2.md) | Current work-item YAML source/output |
| [Project-defined requirement attributes: YAML 0.4](0020-project-attributes-yaml-0.4.md) | YAML 0.4 and project attribute declarations |
| [Requirement semantic output 0.2](0021-requirement-semantic-output-0.2.md) | Requirement output 0.2 |
| [Attribute-aware linking and verification output 0.2](0022-attribute-linking-and-analysis-0.2.md) | Attribute-aware linking and verification output 0.2 |

## Authority boundaries

- The selected source specification controls syntax, validity and semantic values.
- Command contracts control invocation, diagnostics, publication and failure behavior.
- Compiled contracts control serialization, import validation and comparison meaning.
- `versions.properties` declares current independent identifiers consumed by builds.
- The living foundation and roadmap describe direction; adopted behavior requires an
  explicit contract change and migration notes.
