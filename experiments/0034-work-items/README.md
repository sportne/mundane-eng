# Work-item regression fixtures

Current YAML [task and issue examples](../../examples/work-items/work-items.json)
produce the compiled, analyzed and rendered golden outputs in `golden/`.
`make work-verify` checks these alongside dependency, supersession, scoped imports,
evidence resources, failure handling and parser-free consumers. `make work-yaml-verify`
checks YAML presentation and the independent structural schema.

Markdown/JSON-fence source and old compiled work output are unsupported. Fixtures
are reproducible outputs, never an alternative source of task status or narrative.
