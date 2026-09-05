"""Public JVM/native work-item regression contract; no implementation imports."""
import copy
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT=Path(__file__).resolve().parents[1]
NATIVE=str(Path(sys.argv[1]).resolve())
COMMANDS=[['java','-cp',str(ROOT/'build/maintained/classes'),'engineering.work.WorkMain'],[NATIVE]]

def invoke(root,args,status=0):
    runs=[subprocess.run(c+args,cwd=root,capture_output=True,timeout=30) for c in COMMANDS]
    for r in runs:
        assert r.returncode==status,(args,r.returncode,r.stderr.decode(errors='replace'),r.stdout[:500])
    assert runs[0].stdout==runs[1].stdout,(args,'JVM/native output mismatch')
    return json.loads(runs[0].stdout)

def card(ident='TC-1',kind='Task',status='Ready',deps=None,relations=None,body='\n## Question\n\nReview the alarm.\n'):
    metadata={'format':'mundane-work-source-0.1','status':status,'dependencies':deps or [],'relations':relations or [],'planning':dict.fromkeys(['stage','type','condition','unlocks','statusNote'],'')}
    return f'# {kind} {ident}: Review Équipe 😀\n\n```json\n'+json.dumps(metadata,ensure_ascii=False)+'\n```\n'+body

def metadata_change(source,change):
    lines=source.splitlines(keepends=True);m=json.loads(lines[3]);change(m);lines[3]=json.dumps(m)+'\n';return ''.join(lines)

with tempfile.TemporaryDirectory(prefix='mundane-work-') as directory:
    root=Path(directory);source=root/'one.work.md';selection=root/'set.json'
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['one.work.md']}))
    source.write_text(card(),encoding='utf-8');args=['compile','--root','.', 'set.json']
    valid=invoke(root,args);assert valid['complete'] and len(valid['items'])==1
    values=valid['items'][0]['values'];assert values['id']=='TC-1' and values['title']=='Review Équipe 😀' and values['body']=='\n## Question\n\nReview the alarm.\n'
    assert valid['items'][0]['metadataLocation']=={'path':'one.work.md','line':4,'column':1}
    original=source.read_text()
    mutations=[
        ('unknown status',lambda m:m.update(status='Done')),
        ('unknown field',lambda m:m.update(priority='high')),
        ('wrong dependency type',lambda m:m.update(dependencies='TC-2')),
        ('duplicate dependency',lambda m:m.update(dependencies=['TC-2','TC-2'])),
        ('unknown source',lambda m:m.update(format='future')),
        ('invalid relation',lambda m:m.update(relations=[{'relation':'approves','scope':'work','kind':'work-item','target':'TC-2'}])),
        ('remote evidence',lambda m:m.update(relations=[{'relation':'evidence','scope':None,'kind':'resource','target':'https://example.invalid/code'}])),
        ('bad planning',lambda m:m.update(planning={})),
        ('relation limit',lambda m:m.update(dependencies=['T-'+str(i) for i in range(1001)])),
    ]
    for label,change in mutations:
        source.write_text(metadata_change(original,change));a=invoke(root,args,1);assert a['items']==[] and not a['complete'] and a['diagnostics'][0]['location']['line']==4,label
    for invalid in [b'\xff\n',('\ufeff'+original).encode(),original.replace('```\n','').encode(),original[:-1].encode(),original.replace('"status": "Ready"','"status": "Ready", "status": "Ready"').encode(),card(kind='Issue',status='Open',deps=['TC-2']).encode(),card(body='\n').encode()]:
        source.write_bytes(invalid);a=invoke(root,args,1);assert a['items']==[] and not a['complete']
    source.write_bytes(b'x'*(1024*1024+1));assert invoke(root,args,2)['items']==[]
    source.write_bytes(original.replace('\n','\r\n').encode());a=invoke(root,args);assert '\r\n' in a['items'][0]['values']['body']
    source.write_text(original);(root/'two.work.md').write_text(original)
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['two.work.md','one.work.md']}))
    assert invoke(root,args,1)['diagnostics'][0]['code']=='duplicate-work-id'
    (root/'two.work.md').write_text(card('TC-2'))
    ordered=invoke(root,args);assert [x['values']['id'] for x in ordered['items']]==['TC-1','TC-2']
    # Selection order affects selection provenance; semantic inventory order is invariant.
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['one.work.md','two.work.md']}))
    reversed_=invoke(root,args);assert ordered['items']==reversed_['items']
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['missing.work.md']}));assert invoke(root,args,2)['items']==[]
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['one.work.md','one.work.md']}));assert invoke(root,args,1)['items']==[]
    selection.write_text(json.dumps({'format':'mundane-work-set-0.1','files':['one.work.md']}))
    source.write_text(card(body='x'*500000+'\n'))
    for command in COMMANDS:
        p=subprocess.Popen(command+args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        assert p.stdout.read(1);p.stdout.close();assert p.wait(timeout=30) in (2,-13);p.stderr.close()
        def close_stdout():os.close(1)
        r=subprocess.run(command+args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE,preexec_fn=close_stdout,timeout=30);assert r.returncode==2
    source.write_text(original)
print('PASS work compiler: JVM/native values, Unicode points, 9 metadata mutations, physical/size/duplicate/selection failures, CRLF preservation and real broken output')

fixture=invoke(ROOT,['compile','--root','.','examples/work-items/work-items.json'])
expected=json.loads((ROOT/'experiments/0034-work-items/golden/compiled.json').read_text())
assert fixture==expected
assert [i['values']['id'] for i in fixture['items']]==['ISSUE-1','TC-EXAMPLE']
print('PASS independently checked task/issue semantic golden')
