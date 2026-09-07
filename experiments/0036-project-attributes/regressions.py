"""Seeded attribute workflows and compilable, behavior-changing mutations; no external services."""
import copy,json,random,shutil,subprocess,sys,tempfile,time
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'scripts'))
import source_yaml
sys.dont_write_bytecode=True
from workflow import ROOT,BIN,HERE,write,command,plan,imports,analyze,render
START=time.monotonic();SCHEMA=source_yaml.loads((ROOT/'examples/attributes/requirement-attributes.yaml').read_text())

def compile_source(root,status=0,cmd=None):
    args=['--source=yaml-0.4','--root','.','--attribute-schema','schema.yaml','source']
    if cmd:
        p=subprocess.run(cmd+args,cwd=root,capture_output=True,timeout=30);assert p.returncode in (0,1,2);return p.returncode,json.loads(p.stdout)
    return status,json.loads(command(root,'mundanereq-compile',args,status))

def scenario(root,seed):
    rng=random.Random(seed);schema=copy.deepcopy(SCHEMA);rng.shuffle(schema['attributes']['discipline']['values']);write(root/'schema.yaml',schema);(root/'source').mkdir();model={};docs=[]
    for group in range(2):
        records=[]
        for n in range(rng.randint(2,12)):
            id=f'R-{group}-{n:02}';attrs={'discipline':rng.choice(['software','electronics','mechanical'])}
            if rng.choice([True,False]):attrs['owner-team']=rng.choice(['Controls 😀','Firmware <team>','Design \\ "team"','Line 1 / line 2'])
            model[id]=attrs;records.append({'id':id,'title':'Review '+id,'statement':'The logger shall record an observation.','attributes':attrs})
        doc={'format':'mundanereq-yaml-0.4','attributeSchema':schema['name'],'requirements':records};docs.append(copy.deepcopy(doc));write(root/f'source/{group}.mreq.yaml',doc)
    args=['--source=yaml-0.4','--attribute-schema','schema.yaml'];_,baseline=compile_source(root)
    assert {r['values']['id']:r['values']['attributes'] for r in baseline['requirements']}==model
    # Physical byte normalization has an independent expected output, preserving all decoded values.
    for p in sorted((root/'source').iterdir()):
        original=p.read_bytes();p.write_bytes(b'# seed comment\r\n'+original.replace(b'\n',b'\r\n'))
    command(root,'mundanereq-format',args+['--write','source']);snap={p:p.read_bytes() for p in (root/'source').iterdir()};command(root,'mundanereq-format',args+['--write','source']);assert all(p.read_bytes()==b for p,b in snap.items()) and all(b'\r' not in b for b in snap.values())
    _,current=compile_source(root);assert [r['values'] for r in current['requirements']]==[r['values'] for r in baseline['requirements']] and current['attributeSchema']['definition']==baseline['attributeSchema']['definition']
    write(root/'baseline.json',baseline);write(root/'current.json',current);plan(root,model);imports(root);a=analyze(root);assert all(r['state']=='current' for r in a['coverage'])
    initial=render(root);assert render(root)==initial
    # One value edit stales only its explicit binding; a later schema-only edit stales all bindings.
    changed=copy.deepcopy(docs[0]);id=changed['requirements'][0]['id'];changed['requirements'][0]['attributes']['owner-team']='Revised team';write(root/'source/0.mreq.yaml',changed);_,current=compile_source(root);write(root/'current.json',current)
    a=analyze(root,1);assert [r['requirementId'] for r in a['coverage'] if r['state']=='review-stale']==[id]
    schema['attributes']['owner-team']['description']='Revised optional owner description';write(root/'schema.yaml',schema);_,current=compile_source(root);write(root/'current.json',current);a=analyze(root,1);assert all(r['schemaChanged'] for r in a['coverage']);assert render(root)!=initial
    # Incomplete production cannot be laundered through the downstream resolver.
    changed['requirements'][0]['attributes']['discipline']='invalid';write(root/'source/0.mreq.yaml',changed);_,bad=compile_source(root,1);assert not bad['complete'] and bad['requirements']==[];write(root/'current.json',bad)
    result=json.loads(command(root,'mundane-verify',['--root','.','--plan','plan.json','imports.json'],2));assert not result['complete'] and not result['coverage']

seeds=[int(sys.argv[2])] if len(sys.argv)==3 and sys.argv[1]=='--seed' else list(range(130800,130812))
assert not sys.argv[1:] or len(sys.argv)==3 and sys.argv[1]=='--seed','usage: regressions.py [--seed N]'
for seed in seeds:
    with tempfile.TemporaryDirectory(prefix=f'attribute-seed-{seed}-') as tmp:
        try:scenario(Path(tmp),seed)
        except Exception:
            retained=ROOT/f'build/attribute-failure-{seed}';shutil.copytree(tmp,retained,dirs_exist_ok=True);print(f'FAIL seed {seed}; replay with --seed {seed}; inputs retained in {retained}',file=sys.stderr);raise
print('PASS attribute workflow seeds:',','.join(map(str,seeds)))
if sys.argv[1:]:raise SystemExit(0)

with tempfile.TemporaryDirectory(prefix='attribute-mutations-') as tmp:
    root=Path(tmp);(root/'source').mkdir();write(root/'schema.yaml',SCHEMA)
    original={'format':'mundanereq-yaml-0.4','attributeSchema':SCHEMA['name'],'requirements':[{'id':'SYS-001','title':'Logger','statement':'Shall record.','attributes':{'discipline':'software'}}]}
    cases=[('ignored-requiredness','mundanereq/YamlRequirements.java','Boolean.TRUE.equals(mundanereqValue(declaration.getValue(),"required"))','false',lambda d:d['requirements'][0]['attributes'].update({'owner-team':'x'}) or d['requirements'][0]['attributes'].pop('discipline')),
           ('invalid-enum-accepted','mundanereq/YamlRequirements.java','declaration.get("type").equals("enum")&&!((List<?>)declaration.get("values")).contains(v)','false',lambda d:d['requirements'][0]['attributes'].update(discipline='unknown'))]
    results=[];classes=BIN/'classes';jar=ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar';cp=str(classes)+':'+str(jar)
    def mutant(name,file,old,new,main):
        tracked=ROOT/'src/main/java'/file;text=tracked.read_text();assert text.count(old)==1,(name,'mutation site moved')
        folder=root/name;folder.mkdir();java=folder/Path(file).name;java.write_text(text.replace(old,new));r=subprocess.run(['javac','--release','21','-Xlint:all','-Werror','-cp',cp,'-d',str(folder),str(java)],capture_output=True,timeout=30);assert r.returncode==0,(name,r.stderr)
        assert tracked.read_text()==text;return ['java','-cp',str(folder)+':'+cp,main]
    for name,file,old,new,change in cases:
        d=copy.deepcopy(original);change(d);write(root/'source/a.mreq.yaml',d);code,base=compile_source(root,1);assert not base['complete'] and not base['requirements']
        cmd=mutant(name,file,old,new,'mundanereq.cli.CompileMain');code,bad=compile_source(root,cmd=cmd);assert code==0 and bad['complete'] and len(bad['requirements'])==1,(name,code,bad)
        results.append({'mutation':name,'compiled':True,'baseline':[1,False,0],'mutant':[code,bad['complete'],len(bad['requirements'])],'killed':True})
    write(root/'source/a.mreq.yaml',original);_,before=compile_source(root);write(root/'baseline.json',before);schema=copy.deepcopy(SCHEMA);schema['attributes']['owner-team']['description']='Changed unused description';write(root/'schema.yaml',schema);_,after=compile_source(root);write(root/'current.json',after);plan(root,['SYS-001']);imports(root);base=analyze(root,1);assert base['coverage'][0]['schemaChanged']
    name='ignored-schema-meaning';cmd=mutant(name,'engineering/verification/Verifier.java','!java.util.Objects.equals(schemas.get(text(edge.get("baselineScope"))),schemas.get(text(edge.get("currentScope"))))','false','engineering.verification.VerifyMain')
    p=subprocess.run(cmd+['--root','.','--plan','plan.json','imports.json'],cwd=root,capture_output=True,timeout=30);bad=json.loads(p.stdout);assert p.returncode==0 and bad['coverage'][0]['state']=='current'
    results.append({'mutation':name,'compiled':True,'baseline':[1,'review-stale'],'mutant':[p.returncode,bad['coverage'][0]['state']],'killed':True})
    (ROOT/'build/attribute-mutations.json').write_text(json.dumps(results,indent=2)+'\n')
    require=HERE/'results/mutations.json'
    if require.exists():assert results==json.loads(require.read_text())
    assert time.monotonic()-START<240,'bounded attribute regression budget exceeded'
    print('PASS three compiled attribute mutations killed: requiredness, enum acceptance and whole-schema comparison; tracked source unchanged')
