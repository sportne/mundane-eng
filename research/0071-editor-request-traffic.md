# Editor request traffic and freshness

This dated record retains useful impact/performance evidence. Current source formats
and full-gate results are in [YAML authoring verification](0075-yaml-authoring-verification.md).


A two-folder actual VS Code 1.109.5 Extension Host scenario measured process
launch counts before and after TC-1903. Each folder contained the checked-in small
requirement example. Counts measure work requested, not an assumed latency gain.

| Scenario | Before: folder one / two | After: folder one / two |
| --- | --- | --- |
| Six identical cursor queries in folder one | 6 / 6 | 1 / 0 |
| Edit an unselected notes file | 1 / 1 | 0 / 0 |
| Edit a selected requirement in folder one | 1 / 1 | 1 / 0 |
| Concurrent cursor requests, one per folder | 1 / 1; one usable result | 1 / 1; two usable results |

The baseline used the implementation preceding TC-1903; the after run used
per-folder snapshots and bounded response reuse. `editors/vscode/test/traffic-run.js`
recreates the workspace and asserts the after counts through the actual Java
bridge. Current observations are written under ignored `build/`.

Owning Node tests cover shared requests, a four-entry bound, invalidation and
rejection of late results. Existing Extension Host checks exercise unsaved source
and declaration changes, diagnostics repair, navigation, formatting, completion
and hover. The authoritative gate retains both suites and the traffic scenario.
No broad performance, memory or other-platform claim follows from this small
repeatable workload; new optimizations need their own measurement.
