# Research 0057: Work-item linking verification

Date: 2026-09-05. TC-1604.

Implemented bounded `mundane-work analyze` over strict serialized work, requirement
and verification-plan artifacts. Existing import declaration shape and snapshot/
pin rules are reused by this consumer without changing the existing verification
linker's domain contract. Dependency and supersession graphs have separate cycle
checks. Unfinished prerequisite findings preserve authored status and issue closure.
Resource citations establish file existence/provenance only.

Actual JVM/native checks passed for typed requirement/plan/activity/issue links,
local prerequisites, supersession, unknown targets/kinds, malformed/incomplete
serialized work, forged source locations/digests, duplicate scopes, exact pin
mismatch, missing/build/dependency/supersession cycles and absent resource files.
The compiler still passes its earlier cases and the 18-group JVM suite. A separate
classpath with all requirement classes except generated Versions and the work source
compiler removed successfully ran analysis. This checks the serialized boundary,
not only implementation imports.

The [analysis golden](../experiments/0034-work-items/golden/analysis.json) describes
source snapshots of the simple task/issue example. Additional tests import a work
artifact with the same human ID under another explicit scope, and reject an imported
disconnected cycle. Findings are not completion authorization or satisfaction.

Contract refinements add import-selection provenance and explicit aggregate graph/
output bounds. Input resources, including imported work-item citations, are resolved
under the analysis root; callers must provide that layout. No hidden source-root
rebasing or future artifact adapter is inferred. Full clean verification follows
TC-1606; these are focused local results, not hosted CI evidence.
