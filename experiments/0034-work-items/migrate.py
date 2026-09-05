"""Replayable one-time legacy-card conversion; frozen evidence, not a second backlog."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[2]
INVENTORY=Path(__file__).with_name('migration.json')
def sha(data):return hashlib.sha256(data).hexdigest()
def legacy(text):
    start=re.search(r'^## ',text,re.M)
    if not start:raise ValueError('missing narrative')
    prefix,body=text[:start.start()],text[start.start():]
    parts=prefix.rstrip('\n').split('\n\n');heading=parts.pop(0)
    match=re.fullmatch(r'# Task (TC-\d{4}): (.+)',heading)
    if not match:raise ValueError('unexpected legacy heading')
    fields={}
    for block in parts:
        name,sep,value=block.partition(': ')
        if not sep or name in fields:raise ValueError('unexpected legacy metadata block')
        fields[name]=value
    if not {'Status','Roadmap stage','Type','Depends on','Unlocks'} <= fields.keys() or fields.keys()-{'Status','Roadmap stage','Type','Depends on','Unlocks','Completion qualifier','Current disposition'}:raise ValueError('unexpected metadata keys')
    return match[1],heading,fields,body

def convert(text,status_override=None,relations=None):
    ident,heading,f,body=legacy(text)
    raw=f['Status'];status=raw.split(' —',1)[0].split(':',1)[0]
    note=raw if status!=raw else ''
    for label in ['Completion qualifier','Current disposition']:
        if label in f:note+='\n\n'+label+': '+f[label]
    note=note.lstrip('\n')
    m={'format':'mundane-work-source-0.1','status':status_override or status,
       'dependencies':sorted(set(re.findall(r'TC-\d{4}',f['Depends on']))),
       'relations':relations or [],'planning':{'stage':f['Roadmap stage'],'type':f['Type'],'condition':f['Depends on'],'unlocks':f['Unlocks'],'statusNote':note}}
    return heading+'\n\n```json\n'+json.dumps(m,ensure_ascii=False,indent=2)+'\n```\n\n'+body

def baseline_file(commit,path):return subprocess.check_output(['git','show',commit+':'+path],cwd=ROOT)

def check():
    record=json.loads(INVENTORY.read_text());assert re.fullmatch('[0-9a-f]{40}',record['baselineCommit'])
    for item in record['cards']:
        raw=baseline_file(record['baselineCommit'],item['path']);assert sha(raw)==item['beforeSha256']
        ident,_,_,body=legacy(raw.decode());assert ident==item['id'] and sha(body.encode())==item['bodySha256']
        output=convert(raw.decode(),item['statusOverride'],item['addedRelations']);assert sha(output.encode())==item['convertedSha256']
        assert output.endswith(body)
    print('PASS migration replay:',len(record['cards']),'IDs, complete narrative suffixes and explicit metadata transformations from immutable baseline')

if __name__=='__main__':
    if sys.argv[1:]==['--write']:
        if INVENTORY.exists():raise SystemExit('migration inventory already exists; use default replay check')
        pending=[]
        baseline=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();cards=[]
        files=sorted(list((ROOT/'roadmap').glob('task-[0-9]*.md'))+list((ROOT/'roadmap/closed').glob('task-[0-9]*.md')))
        evidence={'TC-1603':'src/main/java/engineering/work/WorkCompiler.java','TC-1604':'src/main/java/engineering/work/WorkGraph.java','TC-1605':'src/main/java/engineering/work/WorkView.java','TC-1606':'specification/0018-work-items-0.1.md'}
        for p in files:
            path=p.relative_to(ROOT).as_posix();raw=baseline_file(baseline,path);ident,_,_,body=legacy(raw.decode())
            override='In progress' if ident=='TC-1606' else None
            expected=raw.decode().replace('Status: Ready\n','Status: In progress\n',1) if override else raw.decode()
            relations=[{'relation':'evidence','scope':None,'kind':'resource','target':evidence[ident]}] if ident in evidence else []
            output=convert(raw.decode(),override,relations);assert output.endswith(body)
            if p.read_text() not in (expected,output):raise ValueError('unreviewed pre-migration changes: '+path)
            pending.append((p,output))
            cards.append({'id':ident,'path':path,'beforeSha256':sha(raw),'bodySha256':sha(body.encode()),'convertedSha256':sha(output.encode()),'statusOverride':override,'addedRelations':relations})
        # Validate the whole inventory before changing any source file.
        for p,output in pending:p.write_text(output)
        INVENTORY.write_text(json.dumps({'baselineCommit':baseline,'scope':'Conversion checkpoint; cards may subsequently evolve. Body bytes and heading preserved at conversion; only metadata changes.','cards':cards},indent=2)+'\n')
        (ROOT/'roadmap/work-items.json').write_text(json.dumps({'format':'mundane-work-set-0.1','files':sorted(c['path'] for c in cards)},indent=2)+'\n')
        (ROOT/'roadmap/work-imports.json').write_text('{"format":"mundane-imports-0.1","imports":[]}\n')
        check()
    elif not sys.argv[1:]:check()
    else:raise SystemExit('usage: migrate.py [--write]')
