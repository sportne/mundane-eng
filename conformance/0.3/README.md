# YAML 0.3 conformance

`corpora.txt` selects checked-in valid projects. `authoring/` exercises YAML block
styles, quotes and opaque content. `invalid-cases.tsv` records source and structural
schema verdicts for `invalid/`. The schema, compiler and public CLI checks all run
through `make verify`. Structural schema validity does not replace source rules.
