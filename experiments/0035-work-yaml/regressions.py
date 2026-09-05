"""Bounded seeded YAML emitter/compile/analyze checks with independently computed values."""
import copy
import importlib.util
import json
from pathlib import Path
import random
import subprocess
import sys
import tempfile
sys.dont_write_bytecode=True

ROOT=Path(__file__).resolve().parents[2]
BINARY=ROOT/'build/maintained/mundane-work'
spec=importlib.util.spec_from_file_location('work_yaml_migration',Path(__file__).with_name('migrate.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

def command(root,args,status=0):
    r=subprocess.run([str(BINARY),*args],cwd=root,capture_output=True,timeout=30)
    assert r.returncode==status,(r.returncode,r.stderr,r.stdout[:1000]);return json.loads(r.stdout)

def write(root,path,value):(root/path).write_text(json.dumps(value,ensure_ascii=False)+'\n')
seeds=[int(sys.argv[2])] if len(sys.argv)==3 and sys.argv[1]=='--seed' else range(160900,160908)
assert not sys.argv[1:] or len(sys.argv)==3 and sys.argv[1]=='--seed'
for seed in seeds:
    try:
        with tempfile.TemporaryDirectory(prefix=f'work-yaml-seed-{seed}-') as folder:
            root=Path(folder);rng=random.Random(seed);expected={};count=rng.randrange(4,25)
            samples=['\n## Heading\n\n  code\n# : [ordinary prose]\n','Text without final newline','Trailing blank lines\n\n\n','Literal CRLF\r\nnext\r\n','Unicode 😀 Équipe\n\tindent\n','Line separator\u2028next\n','NEL\x85content\n','\n\n    leading indentation\n', '```yaml\nstatus: Complete\n```\n']
            for i in range(count):
                ident=f'TC-{i:03}';v={'id':ident,'kind':'task','title':'Review '+ident,'status':rng.choice(['Ready','Planned','Complete']),'dependencies':[f'TC-{j:03}' for j in range(i) if rng.random()<.25],'relations':[],'planning':dict.fromkeys(['stage','type','condition','unlocks','statusNote'],''),'body':samples[i%len(samples)]}
                if i==1:v['dependencies']=['TC-000']
                expected[ident]=v;raw=module.emit(v)
                if seed%2:raw=raw.replace(b'\n',b'\r\n')
                (root/f'{ident}.yaml').write_bytes(raw)
            write(root,'set.json',{'format':'mundane-work-set-0.2','source':'mundane-work-yaml-0.2','files':[f'{i}.yaml' for i in reversed(expected)]})
            args=['compile','--root','.','set.json'];a=command(root,args)
            assert {i['values']['id']:i['values'] for i in a['items']}==expected,'decoded strings/values changed'
            write(root,'work.json',a);write(root,'imports.json',{'format':'mundane-imports-0.1','imports':[]})
            analyze=['analyze','--root','.','--imports','imports.json','work.json'];result=command(root,analyze)
            for f in result['findings']:assert f['unfinishedDependencies']==sorted(d for d in expected[f['id']]['dependencies'] if expected[d]['status']!='Complete')
            # Changing a human ID leaves explicit references unresolved; no hidden alias/identity.
            changed=copy.deepcopy(expected['TC-000']);changed['id']='TC-RENAMED';(root/'TC-000.yaml').write_bytes(module.emit(changed));write(root,'work.json',command(root,args));bad=command(root,analyze,1);assert bad['edges']==[] and bad['findings']==[] and any(d['code']=='missing-work-target' for d in bad['diagnostics'])
            # Independent invalid sources cannot result in partial compiled publication.
            (root/'TC-000.yaml').write_bytes(module.emit(expected['TC-000'])+b'kind: task\n');bad=command(root,args,1);assert not bad['complete'] and bad['items']==[]
    except BaseException:
        print(f'FAIL YAML seed {seed}; replay: python3 experiments/0035-work-yaml/regressions.py --seed {seed}',file=sys.stderr);raise
print('PASS YAML workflow seeds:',','.join(map(str,seeds)),'including exact literal/escaped strings, CRLF, prerequisite findings and invalid publication')
