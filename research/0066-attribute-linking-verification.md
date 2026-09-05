# Research 0066: Attribute linking and analysis

TC-1306 implements strict serialized requirement output 0.2 validation in the
shared artifact boundary. Source/output pairing, declaration rules, requiredness,
values, schema provenance and per-name/value locations are checked before linking.
New attribute fields masquerading as old-format metadata are rejected.

[Linking/analysis 0.2](../specification/0022-attribute-linking-and-analysis-0.2.md)
preserves separate scoped snapshots and exact pins. Findings include changed
attribute names and a schemaChanged flag. The whole canonical definition is review
meaning: optional declarations, descriptions, requiredness and unused enum additions
can stale every compared binding. Comments, paths and enum order alone do not.
Old/schema-free new comparison explicitly promotes to empty attributes/null schema.
All-old workflows retain output 0.1 and existing findings.

The public native/JVM matrix exercises real source recompilation, a checked-in
combined value/description-change golden, forged serialized values/declarations and
locations, incomplete imports, pins, and old/new promotion. Parser-free classpaths
remove requirement and work-item source adapters and the YAML library. Both linking
and an actual YAML task's typed requirement relation still succeed; a malformed
attribute import prevents work edges. No schema merge or inferred assessment/edge
was introduced.

`make verify` exited 0 in the working checkout, including existing scope/ambiguity,
ID correction, output-failure and historical corpus regressions. [Recorded checks](../experiments/0036-project-attributes/results/1306-verify.txt)
are local bounded evidence. Report display is TC-1307 and clean integrated replay is
TC-1308. The conservative policy can cause extra reviews; it makes no test-execution,
approval, evidence-adequacy or satisfaction claim.
