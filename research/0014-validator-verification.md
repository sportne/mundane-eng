# Research 0014: Validator Verification

Status: Complete

Date: 2026-08-23

Roadmap task: [TC-0302](../roadmap/closed/task-0302-verify-validator-behavior.yaml)

## Question

Is `mundanereq-validate` precise and predictable enough to replace the
experimental probe's validation role in ordinary editor and CI use?

## Automated evidence

`make validator-verify` builds the Linux GraalVM native image without fallback
and compares its status, standard output, and standard error byte for byte with
the JVM entry point. The verification covers:

- the 0.1 and 0.2 valid conformance corpora under the current strict 0.2
  contract (0.1 source is a conforming subset of 0.2 source);
- both source-representation layouts;
- the sustained-authoring corpus;
- the NASA FRET transfer corpus;
- all fourteen invalid conformance selections and their expected diagnostic
  categories;
- an explicit source file and an explicit directory;
- duplicate inputs, which select one normalized path;
- an empty directory and an unavailable input;
- no arguments, an unknown option, `--help`, and `--version`; and
- concurrent capture of 1,200 source diagnostics, large enough to exceed a
  typical process pipe buffer; and
- two diagnostic-guided repair sequences.

The maintained JVM regression suite separately retains constructed physical,
syntax, comment, prose, math, identity, and relationship cases that are not
stored as individual conformance files.

## Repair observations

The first repair starts with a prohibited U+007F following a supplementary
Unicode scalar. Both implementations identify line 2, column 10. Removing the
reported character makes the same explicit file conforming.

The second repair starts with a `decomposes: PARENT` relationship whose target
is absent. Both implementations identify the target at line 5, column 13.
Adding a complete `PARENT` record makes the selected source set conforming.

These cases do not prove that every possible diagnostic is ideally worded.
They show that representative physical and source-set failures can be located
and repaired with an ordinary editor using only the plain diagnostic.

## Bounded operational observation

On the development environment, one post-build native process validation took
approximately 1 ms for the one-file 0.2 conformance corpus and 2 ms for the
twenty-file one-record-per-file corpus. These are single observations printed
by the verification test, not benchmarks or asserted thresholds. They reveal
no obvious native-startup or small-repository scan regression and support no
organizational-scale claim.

## CI use

After selecting GraalVM Java 21, a clean checkout can run:

    make validator-verify

The log names each evidence group, retains failures and diagnostics, and exits
nonzero on any JVM/native disagreement. A normal validation-only CI job may
build once with `make native-validator` and then run:

    build/maintained/mundanereq-validate REQUIREMENTS_ROOT...

No generated executable, inventory, index, or database needs to be committed.

## Decision

No conformance or usability blocker was found in this bounded verification.
The maintained validator can replace the experimental probe for strict 0.2
validation. Proceed to the trial-contract card without adding formatter,
trace, policy, machine-readable-output, daemon, or persistent-state behavior.
