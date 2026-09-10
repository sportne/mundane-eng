# Engineering YAML authoring

Engineering support is an optional second bridge. Requirements/work-item projects
keep their existing bridge and source selection. Install the local editor bundle,
set mundane.engineeringExecutable to the absolute extracted
bin/mundane-engineering-editor path, and select a workspace-relative JSON file
through mundane.engineeringProject. The extension executes bridges only in trusted
workspaces. The compiled bridge requires no JVM or Python at runtime.

Selection example:

    {
      "format": "mundane-engineering-editor-project-0.1",
      "files": [
        {"path": "procedure.yaml", "kind": "procedure", "imports": "procedure-imports.json"},
        {"path": "budget.yaml", "kind": "budget", "imports": "budget-imports.json"}
      ]
    }

Each imports file is the same explicit mundane-domain-imports-0.1 selection used by
the domain compiler. Set imports to null when there are none. One file cannot be
selected by multiple authoring domains. Sources are limited to 1 MiB each, 128
files, and a 16 MiB serialized snapshot. Normalized workspace-relative paths,
regular-file checks and realpath containment apply to every selected file.
Missing imports are watched so restoring them refreshes diagnostics.

| Family | Working-copy assistance | Native checks retained by owner |
| --- | --- | --- |
| Existing verification plans | Plain YAML editing; compiled activities available as imported targets | Plan source compilation, local coverage checks and linkage remain with mundane-plan |\n| Architecture | YAML/schema, typed IDs, enums, hover, local/imported navigation | Structural/interface semantics |
| Configuration | YAML/schema, enums, hover | Previous baseline, members, resource publication and revision checks |
| Safety | YAML/schema, typed IDs, enums, hover, navigation | Hazard/control/FMEA/tree consistency and review drift |
| Procedure | YAML/schema, typed IDs, enums, hover, navigation | Scenario semantics, exact configuration and execution |
| Manual observation | YAML/schema, enums, hover | Procedure/subject pins and native observation import |
| Assessment | YAML/schema, disposition completion, hover | Run selection and authored-disposition interpretation |
| Equipment | YAML/schema, typed IDs, enums, hover, navigation | Port compatibility, cable topology and substitutions |
| Budget | YAML/schema, typed IDs, enums, hover, navigation | Unit/range calculations, equipment pins and reliability |
| Software | YAML/schema, typed IDs, enums, hover, navigation | Native SLSA/CycloneDX integrity and build/security analysis |
| Assurance | YAML/schema, typed IDs, enums, hover, navigation | Evidence support, signatures, trust, expiry and obligations |
| Operations | YAML/schema, typed IDs, enums, hover, navigation | Commissioning, incidents and release readiness |
| Change inventory | YAML/schema, enums, hover | Before/after pins, domain comparison and staleness |

The twelve additional source families are selectable in this bridge. Existing\nrequirements, attribute declarations and work items retain their established bridge.\nVerification-plan source assistance is limited to ordinary YAML editing; its compiled\nactivities participate in typed imported navigation.\n\nTyped assistance applies to explicit scope/kind/id references and owned schema enum
scalars. Strings that name scopes, resource paths or related IDs are not silently
treated as typed references. Cross-file navigation uses pinned compiled indexes;
an unsaved target does not replace that selected revision. Matching origin bytes
enable navigation to the exact YAML ID scalar. When names repeat across record\nkinds, matching uses the typed record values; ambiguous matches suppress navigation; missing or changed origins disable
the link while preserving the compiled index. Import validation checks the envelope
and schema, not transitive native resources or reviewer signatures.

Hover and response metadata distinguish working-copy checks from native assessment.
No new engineering formatting is offered: the provider returns no edits, preserving
comments, scalar styles and bytes. Existing requirements formatting remains available.
There is no diagram editor or inferred latest import. Select generated Markdown paths\nwith mundane.engineeringViews and use Mundane: Open Engineering Review View to open\na source-linked report. This opens retained output; rerun the owning CLI to refresh\nits evaluation. Run the linked
[change commands](../../distribution/change.md) and owning domain commands for
derived views and authoritative analysis.

The installed VSIX scenarios exercise all twelve families from the GCS example,
unsaved timing and power errors/repair, typed completion and imported navigation,
dirty origin suppression, missing-import recovery and rapid-edit invalidation.
