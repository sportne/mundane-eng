# Derived verification report

`render.py` consumes a complete compiled verification analysis and emits escaped,
source-linked HTML. It validates the supplied results before rendering. It does not
read or modify authored requirement/plan source or execute verification activities.

The [attribute workflow](../0036-project-attributes/README.md) rebuilds the maintained
example and report. `make attribute-report-verify` checks goldens, value/schema
provenance, tamper rejection and output failures. Old standalone generated reports
have been removed; the active golden belongs to the owning attribute workflow.
