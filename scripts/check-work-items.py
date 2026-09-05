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

with tempfile.TemporaryDirectory(prefix='mundane-work-link-') as directory:
    root=Path(directory)
    def write(name,value): (root/name).write_text(json.dumps(value),encoding='utf-8')
    relations=[{'relation':'addresses','scope':'req','kind':'requirement','target':'A'},
               {'relation':'relates-to','scope':'plans','kind':'verification-plan','target':'PLAN-B'},
               {'relation':'relates-to','scope':'plans','kind':'verification-activity','target':'ACT-ACCESS'},
               {'relation':'addresses','scope':'work','kind':'work-item','target':'ISSUE-1'},
               {'relation':'supersedes','scope':'work','kind':'work-item','target':'TC-OLD'},
               {'relation':'evidence','scope':None,'kind':'resource','target':'evidence.md'}]
    texts=[card('TC-1',deps=['TC-2'],relations=relations),card('TC-2',status='Complete'),card('TC-OLD',status='Superseded'),card('ISSUE-1','Issue','Open')]
    for i,text in enumerate(texts):(root/f'{i}.work.md').write_text(text)
    write('set.json',{'format':'mundane-work-set-0.1','files':[f'{i}.work.md' for i in range(4)]})
    work=invoke(root,['compile','--root','.','set.json']);write('work.json',work)
    requirement=json.loads((ROOT/'specification/examples/requirements-artifact-0.1/valid.json').read_text());write('req.json',requirement)
    plan=json.loads((ROOT/'experiments/0028-verification-contract/fixtures/plan.json').read_text());write('plan.json',plan)
    (root/'evidence.md').write_text('Recorded local evidence citation, not an approval.\n')
    imports={'format':'mundane-imports-0.1','imports':[{'scope':scope,'kind':kind,'path':path,'sha256':None,'dependsOn':[]} for scope,kind,path in [('req','requirements','req.json'),('plans','verification-plan','plan.json')]]}
    write('imports.json',imports);args=['analyze','--root','.','--imports','imports.json','work.json']
    good=invoke(root,args);assert good['complete'] and len(good['edges'])==7
    by_id={f['id']:f for f in good['findings']};assert by_id['TC-1']['unfinishedDependencies']==[] and by_id['ISSUE-1']['status']=='Open'
    def changed_work(change,expected=1):
        a=copy.deepcopy(work);change(a);write('work.json',a);result=invoke(root,args,expected);assert not result['complete'] and result['edges']==[] and result['findings']==[];write('work.json',work);return result
    def values(a,ident):return next(x['values'] for x in a['items'] if x['values']['id']==ident)
    cases=[lambda a:a.update(format='future'),lambda a:a.update(complete=False),lambda a:values(a,'TC-1').update(status='Done'),lambda a:values(a,'TC-1').update(dependencies=['ABSENT']),lambda a:values(a,'TC-2').update(dependencies=['TC-1']),lambda a:values(a,'TC-OLD').update(relations=[{'relation':'supersedes','scope':'work','kind':'work-item','target':'TC-1'}]),lambda a:values(a,'TC-1').update(relations=[]),lambda a:a['items'][0]['metadataLocation'].update(line=0),lambda a:a['sources'][0].update(sha256='bad'),lambda a:a['items'].append(a['items'][0]),lambda a:values(a,'ISSUE-1').update(dependencies=['TC-2'])]
    for change in cases:changed_work(change)
    changed_work(lambda a:values(a,'TC-1')['relations'][0].update(target='ABSENT'))
    changed_work(lambda a:values(a,'TC-1')['relations'][0].update(scope='unknown'))
    changed_work(lambda a:values(a,'TC-1')['relations'][0].update(kind='verification-plan'))
    def changed_import(change,expected=1):
        a=copy.deepcopy(imports);change(a);write('imports.json',a);result=invoke(root,args,expected);assert result['edges']==[] and result['findings']==[];write('imports.json',imports)
    for change in [lambda a:a.update(format='future'),lambda a:a['imports'].append(a['imports'][0]),lambda a:a['imports'][0].update(scope='work'),lambda a:a['imports'][0].update(sha256='0'*64),lambda a:a['imports'][0].update(kind='work-items'),lambda a:a['imports'][0].update(dependsOn=['ABSENT']),lambda a:a['imports'][0].update(dependsOn=['req']),lambda a:a['imports'][0].update(dependsOn=['plans','plans'])]:changed_import(change)
    a=copy.deepcopy(work);values(a,'TC-2')['status']='Planned';write('work.json',a);finding=invoke(root,args);assert next(f for f in finding['findings'] if f['id']=='TC-1')['unfinishedDependencies']==['TC-2'];write('work.json',work)
    (root/'evidence.md').unlink();assert invoke(root,args,2)['edges']==[]
    # Artifact consumers validate serialized contracts without access to source parsing.
    (root/'evidence.md').write_text('Evidence restored.\n')
    import shutil
    isolated=root/'classes';shutil.copytree(ROOT/'build/maintained/classes',isolated)
    for p in (isolated/'mundanereq').iterdir():
        if p.name!='Versions.class':
            if p.is_dir():shutil.rmtree(p)
            else:p.unlink()
    for p in (isolated/'engineering/work').glob('WorkCompiler*.class'):p.unlink()
    r=subprocess.run(['java','-cp',str(isolated),'engineering.work.WorkMain']+args,cwd=root,capture_output=True,timeout=30)
    assert r.returncode==0,r.stderr
print('PASS work analysis: typed requirement/plan/activity/issue links, prerequisite findings, supersession, 22+ invalid serialized/import cases and parser-free execution')

with tempfile.TemporaryDirectory(prefix='work-import-scope-') as directory:
    root=Path(directory)
    (root/'a.md').write_text(card('TC-1'))
    (root/'set.json').write_text(json.dumps({'format':'mundane-work-set-0.1','files':['a.md']}))
    a=invoke(root,['compile','--root','.','set.json'])
    (root/'other.json').write_text(json.dumps(a))
    primary=copy.deepcopy(a);primary['items'][0]['values']['relations']=[{'relation':'relates-to','scope':'other','kind':'work-item','target':'TC-1'}]
    (root/'primary.json').write_text(json.dumps(primary))
    (root/'imports.json').write_text(json.dumps({'format':'mundane-imports-0.1','imports':[{'scope':'other','kind':'work-items','path':'other.json','sha256':None,'dependsOn':[]}]}))
    args=['analyze','--root','.','--imports','imports.json','primary.json']
    out=invoke(root,args);assert out['edges'][0]['from']=='work:work-item:TC-1' and out['edges'][0]['to']=='other:work-item:TC-1'
    broken=copy.deepcopy(a);broken['items'][0]['values']['dependencies']=['TC-1'];(root/'other.json').write_text(json.dumps(broken))
    assert invoke(root,args,1)['diagnostics'][0]['code']=='dependency-cycle'
print('PASS imported work-item scopes preserve equal human IDs and validate disconnected dependency graphs')

raw=subprocess.check_output(COMMANDS[1]+['compile','--root','.','examples/work-items/work-items.json'],cwd=ROOT,timeout=30)
(ROOT/'build/work-example.json').write_bytes(raw)
analysis=invoke(ROOT,['analyze','--root','.','--imports','examples/work-items/imports.json','build/work-example.json'])
assert analysis==json.loads((ROOT/'experiments/0034-work-items/golden/analysis.json').read_text())
print('PASS deterministic work analysis golden')

# Rendering verifies the serialized findings before emitting any Markdown.
with tempfile.TemporaryDirectory(prefix='work-view-') as directory:
    root=Path(directory);source=root/'analysis.json'
    source.write_text(json.dumps(analysis))
    args=['view','--root','.','analysis.json']
    def view(value,status=0):
        source.write_text(json.dumps(value));results=[subprocess.run(c+args,cwd=root,capture_output=True,timeout=30) for c in COMMANDS]
        assert all(r.returncode==status for r in results),[(r.returncode,r.stderr) for r in results]
        assert results[0].stdout==results[1].stdout
        if status:assert results[0].stdout==b''
        return results[0].stdout
    rendered=view(analysis)
    assert rendered==(ROOT/'experiments/0034-work-items/golden/view.txt').read_bytes()
    assert b'Derived work-item index' in rendered and b'[ISSUE-1]' in rendered and b'## Reverse navigation' in rendered
    source.unlink();assert view(analysis)==rendered
    for change in [lambda a:a.update(format='future'),lambda a:a.update(complete=False),lambda a:a.update(edges=[]),lambda a:a['findings'][0].update(status='Closed'),lambda a:a.update(resources=[{'path':'fake.md','sha256':'0'*64}]),lambda a:a['workArtifact']['artifact']['items'][0]['values'].update(status='bogus')]:
        value=copy.deepcopy(analysis);change(value);view(value,1)
    view([],1)
    hostile=copy.deepcopy(analysis);hostile['workArtifact']['artifact']['items'][0]['values']['title']='<script>alert(1)</script> | [click](javascript:bad)'
    safe=view(hostile);assert b'<script>' not in safe and b'javascript:bad)' in safe and b'&#60;script&#62;' in safe and b'&#124;' in safe and b'&#91;click&#93;' in safe
    source.write_text(json.dumps(analysis))
    for command in COMMANDS:
        p=subprocess.Popen(command+args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # Small views may fit a pipe before it closes; force a larger valid title.
        p.communicate(timeout=30);assert p.returncode==0
        large=copy.deepcopy(analysis);large['workArtifact']['artifact']['items'][0]['values']['title']='X'*200000
        source.write_text(json.dumps(large));p=subprocess.Popen(command+args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        assert p.stdout.read(1);p.stdout.close();assert p.wait(timeout=30) in (2,-13);p.stderr.close()
        source.write_text(json.dumps(analysis))
print('PASS derived view: exact golden/rebuild, tampered-analysis rejection, hostile text escaping, parser-independent source links and actual broken output')
