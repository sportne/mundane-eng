# Tool safety and YAML command addendum

Status: Normative additive command contract

Requirement commands default to YAML 0.3. A leading `--source=yaml-0.3` or
`--source=yaml-0.4` explicitly selects a profile and precedes all other options.
`--version` describes the selected source contract.

YAML 0.4 adds explicit project attributes through [0020](0020-project-attributes-yaml-0.4.md).
A `--` delimiter before input paths ends option parsing, including for trace after
its operation and ID. Subsequent names such as `--help` are literal paths.

## Output completion

All requirement commands check stdout and stderr completion on
normal, help, version, usage and source-diagnostic paths. Partial writes, flush
failure and closed streams yield exit 2, including when validation otherwise yields
1. When stdout fails and stderr is usable, a diagnostic is attempted on stderr.
If stderr also fails, the exit status remains non-success without recursive fallback.
SIGPIPE termination is a permitted platform non-success. No output delivery is
promised through a failed stream.

## Formatter replacement

The source selection records bytes and the filesystem file key when available.
Before replacing each changed file, the formatter checks that it remains a regular
file with the same available file key and exact original bytes. It checks again
before the non-atomic fallback move. Detected external changes are operational
failure and remain untouched. Deletion/replacement also fails rather than silently
recreating or overwriting the file. No-change files require no writes.

This is a pre-replacement check, not portable compare-and-swap: a race remains
between checking and rename, and file keys may be absent or reused. Timestamp-only
comparison is not used. Existing POSIX permission preservation and temporary-file
cleanup remain. The fallback move retains its previously documented atomicity limits.

A replacement failure stops the batch. The diagnostic names the failed path and
lists prior Changed/Unchanged paths and later Unprocessed paths. Earlier completed
writes remain in place. Refresh the source selection and retry after resolving the
failure; already formatted files remain unchanged. Full source-set validation still
precedes all formatter writes. A failed summary delivery can follow completed writes
and returns exit 2; callers must inspect files before retrying.
