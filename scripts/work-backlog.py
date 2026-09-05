"""Build/check the repository's disposable work-item index through public commands."""
import json
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
BINARY=ROOT/'build/maintained/mundane-work'
OUT=ROOT/'build/work-backlog'
OUT.mkdir(parents=True,exist_ok=True)

def invoke(*args):
    result=subprocess.run([str(BINARY),*args],cwd=ROOT,capture_output=True,timeout=60)
    if result.returncode:raise SystemExit(result.stderr.decode(errors='replace')+result.stdout.decode(errors='replace'))
    return result.stdout

def rebuild():
    compiled=invoke('compile','--root','.','roadmap/work-items.json');(OUT/'items.json').write_bytes(compiled)
    analysis=invoke('analyze','--root','.','--imports','roadmap/work-imports.json','build/work-backlog/items.json');(OUT/'analysis.json').write_bytes(analysis)
    return compiled,analysis,invoke('view','--root','.','build/work-backlog/analysis.json')

compiled,analysis,view=rebuild()
selection=json.loads((ROOT/'roadmap/work-items.json').read_text())
actual=sorted(p.relative_to(ROOT).as_posix() for base in [ROOT/'roadmap',ROOT/'roadmap/closed'] for pattern in ['task-[0-9]*.yaml','issue-*.yaml'] for p in base.glob(pattern))
assert not [p for base in [ROOT/'roadmap',ROOT/'roadmap/closed'] for pattern in ['task-[0-9]*.md','issue-*.md'] for p in base.glob(pattern)],'legacy Markdown cards must not compete with migrated YAML sources'
assert sorted(selection['files'])==actual,'every repository task card must be explicitly selected'
original=json.loads((ROOT/'experiments/0034-work-items/migration.json').read_text())
ids={i['values']['id'] for i in json.loads(compiled)['items']}
assert {i['id'] for i in original['cards']}<=ids,'historical migrated IDs must remain available'
if sys.argv[1:]==['--write']:
    target=ROOT/'WORK-ITEMS.md';temporary=target.with_suffix('.md.tmp');temporary.write_bytes(view);temporary.replace(target)
elif sys.argv[1:]:raise SystemExit('usage: work-backlog.py [--write]')
else:
    assert (ROOT/'WORK-ITEMS.md').read_bytes()==view,'derived index is stale; run make work-index'
    for p in OUT.iterdir():p.unlink()
    assert rebuild()==(compiled,analysis,view),'source-to-view rebuild differs'
print('PASS',len(ids),'repository work items: explicit inventory, strict analysis and '+('updated derived index' if sys.argv[1:] else 'identical delete/rebuild'))
