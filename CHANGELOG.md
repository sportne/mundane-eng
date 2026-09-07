# Changelog

## Unreleased

- Added canonical equipment/BOM/wiring and dimensional interval budgets with
  conditional repair/availability calculations and adversarial GCS workflows.
- Added native SLSA/in-toto and CycloneDX SBOM/VEX adapters with exact build,
  configuration and security-review traceability.

- Added independently built architecture, configuration, safety, procedure and
  evidence commands, with typed compiled imports and source-linked inspection.
- Configuration publication retains exact inputs; safety analysis exposes coverage
  and review gaps. Procedures now drive bounded in-memory simulation, native result
  import, synthetic manual observations and separate authored assessments.
- New engineering source profiles opt into finite numeric YAML quantities. Existing
  requirement, attribute, plan and work source contracts remain unchanged.

- Renamed the project and repository to Mundane-Eng; existing command, format,
  Java package and editor extension identifiers remain compatible.
- Enforced explicit component build dependencies and moved the editor bridge and
  its integration tests under `editors/bridge`.
- Added the GCS reference-system decision, runnable current-tool seed, artifact
  ownership decision and tested architecture/configuration design probes. The new
  domain formats remain example designs pending production implementation.
- Added `make clean` for disposable builds, downloaded dependencies and test caches;
  refreshed current capability and completion documentation.

- Removed the legacy Markdown/JSON-fence work-item source and compiled work output
  0.1 compatibility path. YAML source/selection/output remain 0.2; work CLI is 0.3.
  Current work examples and regression fixtures are YAML. Obsolete reports and
  superseded editor verification narratives were removed; Git retains their history.

- Attribute declarations now use YAML source `mundanereq-attributes-yaml-0.1`.
  The former JSON declaration source is removed. Select the `.yaml` declaration
  explicitly; normalized compiled definitions remain `mundanereq-attribute-schema-0.1`.
- Verification planning now selects one `plan.yaml` with format
  `mundane-plan-yaml-0.1` and plans/activities/coverage sequences. The three TSV
  source files and their parser are removed. Rebuild compiled plans from YAML.
  Plan CLI is 0.2; the compiled plan shape remains 0.1.
- Editor bundle 0.2.0 reads YAML declarations, including unsaved buffers. Install
  the paired bridge/VSIX. No legacy declaration or plan source adapter is retained.

- Editor bundle 0.1.3 adds explicit compiled imports and source mappings for work-item
  relations. Imported requirement/task targets gain revision-checked navigation and
  compiled completion/hover; unavailable or changed source blocks navigation.
  Existing source and compiled artifact formats are unchanged.

- Editor bundle 0.1.2 adds explicit YAML work-item selection, unsaved diagnostics,
  local dependency navigation, status/relation/ID completion and literal hover.
  Requirements and task cards can be edited together; their source and compiled
  contracts are unchanged. Use the matching bridge for the additive work source.

- Removed the custom requirements language (source 0.1/0.2), its parser, formatter
  branch, source fixtures, migration command and ReqIF prototype. YAML 0.3 is now
  the default; YAML 0.4 supports project-defined attributes. The `custom-0.2`
  selector, `mundanereq-source-0.2` artifacts and standalone `.mreq` discovery are
  no longer supported. Convert any remaining source using a previous revision
  before updating; this checkout contains no migration adapter.
- Removed historical experiments, reports and task cards tied to that language.
  Retained feature cards keep their human IDs with current documentation and
  prerequisite links. The verification gate now exercises YAML authoring and the
  independent artifact consumers. Git history has not been rewritten.
