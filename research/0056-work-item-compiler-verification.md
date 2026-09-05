# Research 0056: Work-item compiler verification

Date: 2026-09-05. TC-1603.

Implemented `mundane-work compile --root DIRECTORY MANIFEST` as an independent
Java/GraalVM component using shared bounded JSON, snapshot and output plumbing.
It emits versioned complete-or-empty work-item artifacts; source/metadata failures
retain honest declaration-level locations. A separate serialized validator checks
all fields, identity, status, relationships, locations and source inventories.
Neither consumer nor compiler depends on requirement parser classes.

Actual local `make work-verify` passed under GraalVM CE 21.0.2 on Linux x86-64:
18 JVM test groups, native work executable and public JVM/native compiler checks.
Cases include nine metadata mutations, malformed Unicode/fences/termination,
duplicate keys/IDs/selection, oversized input, CRLF body preservation, deterministic
semantic order, closed stdout and a broken pipe after an output prefix. Direct Java
stream tests cover closed stderr and partial output; unused closed OS stderr is not
claimed detectable. Example task/issue values and source points have an independently
checked [golden](../experiments/0034-work-items/golden/compiled.json).

Independent version declarations supply source, selection, output, analysis and CLI
identifiers. Analysis/view execution is not enabled until its owning cards land.
No source migration has occurred yet. Full integrated clean-checkout verification
is reserved for TC-1606; this record claims the focused checks actually run.
