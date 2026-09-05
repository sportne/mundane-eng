"""Relative documentation links and unique, indexed source-card identities."""
from pathlib import Path
import json
import re
import subprocess
import urllib.parse

ROOT=Path(__file__).resolve().parents[1]
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard'],cwd=ROOT,text=True).splitlines()
artifact=json.loads((ROOT/'build/work-backlog/items.json').read_text())
source_text={i['location']['path']:i['values']['body']+'\n'+'\n'.join(i['values']['planning'].values()) for i in artifact['items']}
missing=[]
for name in set(files):
    p=ROOT/name
    if not p.exists() or p.suffix!='.md' and name not in source_text and name!='roadmap/task-card-template.yaml':continue
    for dest in re.findall(r'\]\(([^)\n]+)\)',source_text.get(name,p.read_text())):
        bare=dest.split('#')[0]
        if not bare or '://' in bare or bare.startswith(('mailto:','/')) or any(x in bare for x in ['*','`',' ']):continue
        if not (p.parent/urllib.parse.unquote(bare)).exists():missing.append((name,dest))
assert not missing,missing
artifact=json.loads((ROOT/'build/work-backlog/items.json').read_text())
analysis=json.loads((ROOT/'build/work-backlog/analysis.json').read_text())
assert artifact['complete'] and analysis['complete']
ids=[i['values']['id'] for i in artifact['items']];assert len(ids)==len(set(ids))
view=(ROOT/'WORK-ITEMS.md').read_text()
for ident in ids:
    assert len(re.findall(r'^\| \['+re.escape(ident)+r'\]\(',view,re.M))==1,ident
index=(ROOT/'roadmap/0002-task-card-index.md').read_text();assert '| Card | Outcome | Status | Depends on |' not in index
assert 'WORK-ITEMS.md' in index
print('PASS relative Markdown links;',len(ids),'unique source IDs indexed once; complete dependency analysis; no duplicate manual status tables')
