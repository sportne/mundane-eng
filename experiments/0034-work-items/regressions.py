"""Bounded seeded workflow checks and three compilable implementation mutations."""
import hashlib
import json
from pathlib import Path
import random
import subprocess
import sys
import tempfile
import time

ROOT=Path(__file__).resolve().parents[2]
BINARY=ROOT/'build/maintained/mundane-work'
CLASSES=ROOT/'build/maintained/classes'
START=time.monotonic()

def run(root,args,command=None):
    assert time.monotonic()-START<180,'work regression budget exceeded'
    r=subprocess.run((command or [str(BINARY)])+args,cwd=root,capture_output=True,timeout=30)
    assert r.returncode in (0,1,2),(r.returncode,r.stderr)
    return r.returncode,json.loads(r.stdout)

def write(root,name,value): (root/name).write_text(json.dumps(value,ensure_ascii=False),encoding='utf-8')
def source(ident,status,deps,body):
    m={'format':'mundane-work-source-0.1','status':status,'dependencies':deps,'relations':[], 'planning':dict.fromkeys(['stage','type','condition','unlocks','statusNote'],'')}
    return '# Task '+ident+': Review '+ident+'\n\n```json\n'+json.dumps(m)+'\n```\n'+body

def scenario(root,seed):
    rng=random.Random(seed);count=rng.randint(4,24);model={};files=[]
    for i in range(count):
        ident=f'TC-{i:03}';deps=[f'TC-{j:03}' for j in range(i) if rng.random()<.2]
        if i==1:deps=['TC-000']
        status=rng.choice(['Complete','Planned','Ready']);body='\n## Work\n\n'+rng.choice(['Prose 😀.','Literal [link](nothing) in opaque prose.','```json\n{"status":"not metadata"}\n```'])+'\n'
        model[ident]={'status':status,'dependencies':deps,'body':body};name=f'card {i:02}.md';files.append(name);text=source(ident,status,deps,body)
        if seed%2:text=text.replace('\n','\r\n');model[ident]['body']=body.replace('\n','\r\n')
        (root/name).write_bytes(text.encode())
    write(root,'set.json',{'format':'mundane-work-set-0.1','files':list(reversed(files))});write(root,'imports.json',{'format':'mundane-imports-0.1','imports':[]})
    code,compiled=run(root,['compile','--root','.','set.json']);assert code==0
    for item in compiled['items']:
        v=item['values'];expected=model[v['id']]
        assert v['body']==expected['body'] and v['status']==expected['status'] and v['dependencies']==sorted(expected['dependencies'])
    write(root,'work.json',compiled);args=['analyze','--root','.','--imports','imports.json','work.json'];code,analysis=run(root,args);assert code==0
    for f in analysis['findings']:
        expected=sorted(d for d in model[f['id']]['dependencies'] if model[d]['status']!='Complete');assert f['unfinishedDependencies']==expected
    # A human ID correction does not synthesize an alias for an existing dependency.
    p=root/files[0];p.write_bytes(p.read_bytes().replace(b'# Task TC-000:',b'# Task TC-RENAMED:',1));code,changed=run(root,['compile','--root','.','set.json']);assert code==0;write(root,'work.json',changed)
    code,failed=run(root,args);assert code==1 and not failed['edges'] and failed['diagnostics'][0]['code']=='missing-work-target'

seeds=[int(sys.argv[2])] if len(sys.argv)==3 and sys.argv[1]=='--seed' else list(range(160600,160608))
assert not sys.argv[1:] or len(sys.argv)==3 and sys.argv[1]=='--seed','usage: regressions.py [--seed N]'
for seed in seeds:
    with tempfile.TemporaryDirectory(prefix=f'work-seed-{seed}-') as tmp:
        try:scenario(Path(tmp),seed)
        except Exception:
            import shutil
            retained=ROOT/f'build/work-regression-failure-{seed}'
            shutil.copytree(tmp,retained,dirs_exist_ok=True)
            print(f'FAIL seed {seed}; inputs retained at {retained}; replay: python3 experiments/0034-work-items/regressions.py --seed {seed}',file=sys.stderr);raise
print('PASS work workflow seeds:',','.join(map(str,seeds)))
if sys.argv[1:]:raise SystemExit(0)

with tempfile.TemporaryDirectory(prefix='work-mutations-') as tmp:
    root=Path(tmp)
    (root/'a.md').write_text(source('TC-A','Ready',['TC-B'],'\nA\n'))
    (root/'b.md').write_text(source('TC-B','Planned',[],'\nB\n'))
    (root/'bad.md').write_text(source('TC-BAD','invalid',[],'\nBad\n'))
    write(root,'set.json',{'format':'mundane-work-set-0.1','files':['a.md','b.md']})
    write(root,'invalid.json',{'format':'mundane-work-set-0.1','files':['a.md','bad.md']})
    status,artifact=run(root,['compile','--root','.','set.json']);assert status==0;write(root,'work.json',artifact)
    write(root,'imports.json',{'format':'mundane-imports-0.1','imports':[]})
    write(root,'pinned.json',{'format':'mundane-imports-0.1','imports':[{'scope':'other','path':'work.json','kind':'work-items','sha256':'0'*64,'dependsOn':[]}]})
    cases=[
        ('partial-publication','WorkCompiler','status==0?new ArrayList<>(items.values()):List.of()','new ArrayList<>(items.values())',['compile','--root','.','invalid.json'],lambda c,a:(c,len(a['items'])),(1,0)),
        ('ignored-prerequisite','WorkGraph','if(!"Complete".equals(map(target.get("values")).get("status")))unfinished.add(dep);','if(false)unfinished.add(dep);',['analyze','--root','.','--imports','imports.json','work.json'],lambda c,a:(c,next(f['unfinishedDependencies'] for f in a['findings'] if f['id']=='TC-A')),(0,['TC-B'])),
        ('ignored-pin','WorkAnalyzer','!digest(entry.get("sha256")).equals(snap.sha256())','false',['analyze','--root','.','--imports','pinned.json','work.json'],lambda c,a:(c,a['complete']),(1,False)),
    ]
    results=[]
    for name,cls,old,new,args,signature,expected in cases:
        baseline=signature(*run(root,args));assert baseline==expected,(name,baseline,expected)
        text=(ROOT/f'src/main/java/engineering/work/{cls}.java').read_text();assert text.count(old)==1
        folder=root/name;folder.mkdir();java=folder/f'{cls}.java';java.write_text(text.replace(old,new))
        javac=subprocess.run(['javac','--release','21','-Xlint:all','-Werror','-cp',str(CLASSES),'-d',str(folder),str(java)],capture_output=True,timeout=30)
        assert javac.returncode==0,(name,javac.stderr)
        command=['java','-cp',str(folder)+':'+str(CLASSES),'engineering.work.WorkMain']
        actual=signature(*run(root,args,command));assert actual!=expected,(name,'survived')
        results.append({'mutation':name,'compiled':True,'baseline':baseline,'mutant':actual,'killed':True})
    results=json.loads(json.dumps(results))
    output=ROOT/'build/work-mutations.json';output.write_text(json.dumps(results,indent=2)+'\n')
    expected=json.loads((ROOT/'experiments/0034-work-items/results/mutations.json').read_text()) if (ROOT/'experiments/0034-work-items/results/mutations.json').exists() else None
    if expected is not None:assert results==expected
    print('PASS three compiled behavioral mutations killed; tracked source unchanged; evidence:',output.relative_to(ROOT))
