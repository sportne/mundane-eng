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
def markdown_anchors(text):
    anchors=set(re.findall(r'<a\s+(?:id|name)=["\']([^"\']+)',text))
    counts={}
    for title in re.findall(r'^#{1,6} +(.+?)\s*#*$',text,re.M):
        slug=re.sub(r'[^\w -]','',title.lower()).replace(' ','-')
        number=counts.get(slug,0);counts[slug]=number+1
        anchors.add(slug+(f'-{number}' if number else ''))
    return anchors

missing=[]
for name in set(files):
    p=ROOT/name
    if not p.exists() or p.suffix!='.md' and name not in source_text and name!='roadmap/task-card-template.yaml':continue
    for dest in re.findall(r'\]\(([^)\n]+)\)',source_text.get(name,p.read_text())):
        bare,separator,fragment=dest.partition('#')
        if '://' in bare or bare.startswith(('mailto:','/')) or any(x in bare for x in ['*','`',' ']):continue
        target=p.parent/urllib.parse.unquote(bare) if bare else p
        if not target.exists():missing.append((name,dest))
        elif fragment and target.suffix=='.md' and not re.fullmatch(r'L[0-9]+(?:-L[0-9]+)?',fragment):
            if urllib.parse.unquote(fragment) not in markdown_anchors(target.read_text()):missing.append((name,dest))
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
print('PASS relative Markdown paths/anchors;',len(ids),'unique source IDs indexed once; complete dependency analysis; no duplicate manual status tables')

# Every numbered contract/decision/experiment must be discoverable from its index.
for folder, pattern in [('specification', '[0-9][0-9][0-9][0-9]-*.md'),
                        ('research', '[0-9][0-9][0-9][0-9]-*.md'),
                        ('experiments', '[0-9][0-9][0-9][0-9]-*/README.md')]:
    directory = ROOT / folder
    index_text = (directory / 'README.md').read_text()
    targets = {urllib.parse.unquote(link.split('#')[0])
               for link in re.findall(r'\]\(([^)\n]+)\)', index_text)}
    unindexed = [str(path.relative_to(directory)) for path in sorted(directory.glob(pattern))
                 if str(path.relative_to(directory)) not in targets]
    assert not unindexed, (folder, 'unindexed documentation', unindexed)
print('PASS complete specification, research and experiment indexes')

# Prose citations must not silently outlive their source cards.
for name, text in source_text.items():
    unresolved = set(re.findall(r'\bTC-[0-9]{4}\b', text)) - set(ids)
    assert not unresolved, (name, 'unresolved task citations', sorted(unresolved))
print('PASS source-card prose task citations resolve')
