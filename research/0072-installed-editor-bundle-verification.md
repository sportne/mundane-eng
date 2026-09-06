# Installed editor bundle verification

The complete `scripts/run-ci-verification.sh` wrapper passed locally after the
TC-1901–TC-1905 consolidation batch. The environment used Ubuntu 24.04 x86-64
under WSL, GraalVM CE 21.0.2, Node.js 22.18.0 and VS Code 1.109.5 under Xvfb.
This records local checks, not hosted CI for the new commits or a Remote/WSL
extension-server compatibility result.

The gate passed twelve maintained Java groups, source/schema and native package
checks, independent artifact workflows, impact properties/mutations, version
checks, six Node tests, development-host provider checks, and the two-folder
[request traffic scenario](0071-editor-request-traffic.md). It rejected both
injected CI faults at their intended targets and restored exact fixture bytes.

The versioned editor bundle includes a matching VSIX/native bridge, independent
protocol/build/project metadata, dependency provenance, runtime notices and a
complete checksum inventory. Checks rejected incompatible VSIX metadata, a bridge
version mismatch, unsupported platforms and altered archive bytes. Reassembly from
identical inputs produced identical archive bytes; no cross-toolchain native-image
or VSIX build reproducibility claim follows.

The installed test verified and extracted the archive, installed its VSIX through
the pinned VS Code CLI into a fresh extensions directory and user profile, and
loaded the target extension from that installed path. Only an empty test harness
was loaded as a development extension. The configured executable was the extracted
bridge. Tests exercised missing executables, incompatible protocol responses,
invalid selections and recovery, then the full diagnostics/navigation/formatting/
completion/hover workflow with unsaved source and declaration changes.

The missing-executable scenario exposed an early stdin-write broken-pipe message.
Deferring request delivery until the child emits `spawn` removed that message; the
full installed test and process-boundary tests passed after the change.

Current output is under `build/editor-package/`; the wrapper records logs and
environment under `build/ci-evidence/`. No user VS Code profile was modified, no
Marketplace upload was performed, and no release was published. Follow the
[bundle guide](../distribution/editor-bundle.md) to install the local artifacts.
