# Tool safety and YAML command addendum

Status: Normative additive command contract

This addendum records current changes to the historical validator/formatter/trace
trial contracts. Default source 0.2 invocations and their successful outputs remain
compatible. A leading `--source=yaml-0.3` selects the new requirements contract;
`--source=custom-0.2` selects the default explicitly. The selector precedes all other
options/operations. `--version` describes the selected source contract.

YAML 0.4 adds explicit project attributes through [0020](0020-project-attributes-yaml-0.4.md).
A `--` delimiter before input paths ends option parsing, including for trace after
its operation and ID. Subsequent names such as `--help` are literal paths.

## Output completion

All three tools and the migration utility check stdout and stderr completion on
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

## Migration utility

Build with `make native-migrate`. The separately installable
`build/maintained/mundanereq-migrate` accepts:

```text
mundanereq-migrate [--dry-run] NEW_OUTPUT_DIRECTORY SOURCE...
```

Sources use the existing strict source 0.2 interpreter. The output directory MUST
not exist; its parent must exist. Input filenames are flattened, replacing a final
`.mreq` with `.mreq.yaml` (otherwise appending `.mreq.yaml`). Collisions fail before
output. All output is parsed under YAML 0.3 and compared with the complete original
semantic model before directory creation. Dry-run performs those checks without
writing anything. Output files use CREATE_NEW and originals are never written.

The converter preserves each file's record order and every comment line in original
sequence at that file's YAML header. Source 0.2 assigns comments no semantic
attachment; relocation is explicit and should be reviewed by authors. All values
are quoted, with escapes for opaque math newlines; source layout is not retained.
IDs, paragraph order, math payloads and relationship sets are compared exactly.

An interrupted or failed write may leave the new directory and earlier/partial
output files; no rollback is claimed. Each completed path is reported as Created.
The original input remains intact. Inspect the failed output directory and rerun to
a new directory after resolving the cause. The converter does not replace existing
projects automatically. Use explicit source selectors to validate the desired copy.
The migration utility is built separately from the existing three-tool trial archive.
