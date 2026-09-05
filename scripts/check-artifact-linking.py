"""Published-artifact resolver matrix; JVM/native and parser-free consumer evidence."""
import copy
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FIXTURE=ROOT/'experiments/0028-verification-contract'
NATIVE=Path(sys.argv[1]).resolve()
ISOLATED=ROOT/'build/artifact-only-classes'
if ISOLATED.exists():shutil.rmtree(ISOLATED)
ISOLATED.mkdir(parents=True)
# Compile only the serialized linker boundary; unrelated source adapters are not dependencies.
subprocess.run(['javac','--release','21','-d',str(ISOLATED)]+[str(p) for p in sorted((ROOT/'src/main/java/engineering/artifacts').glob('*.java'))]+[str(ROOT/'build/maintained/generated/mundanereq/Versions.java')],check=True)
COMMANDS=[['java','-cp',str(ISOLATED),'engineering.artifacts.LinkMain'],[str(NATIVE)]]


def write(path,obj):path.write_text(json.dumps(obj,sort_keys=True,ensure_ascii=False,separators=(',',':'))+'\n')

def invoke(command,root,manifest='fixtures/imports.json',plan='fixtures/plan.json',context=None,status=0,code=None):
    args=command+['--root',str(root),'--plan',str(root/plan)]
    if context is not None:args+=['--context',context]
    process=subprocess.run(args+[str(root/manifest)],capture_output=True,timeout=30)
    assert process.returncode==status,(process.returncode,process.stdout[-600:],process.stderr)
    assert not process.stderr,process.stderr
    result=json.loads(process.stdout)
    assert result['complete']==(status==0)
    if code:
        assert result['diagnostics'][0]['code']==code,result['diagnostics']
        assert not result['edges'] and not result['inverse']
    return result,process.stdout


for command in COMMANDS:
    linked,raw=invoke(command,FIXTURE)
    assert len(linked['edges'])==57 and len(linked['inverse'])==57
    assert {e['requirementId'] for e in linked['edges']}=={r['values']['id'] for r in linked['imports'][0]['artifact']['requirements']}
    assert all(e['location']['path']=='plan:source/coverage.tsv' for e in linked['edges'])
assert invoke(COMMANDS[0],FIXTURE)[1]==invoke(COMMANDS[1],FIXTURE)[1]
with tempfile.TemporaryDirectory(prefix='link-matrix-') as directory:
    root=Path(directory);shutil.copytree(FIXTURE/'fixtures',root/'fixtures')
    original={p.name:json.loads(p.read_text()) for p in (root/'fixtures').glob('*.json')}
    def reset():
        for name,obj in original.items():write(root/'fixtures'/name,obj)
    # The fixture pin hashes the original serialized bytes, preserved by write().
    def check(code,change,status=1):
        reset();change()
        outputs=[invoke(c,root,status=status,code=code)[1] for c in COMMANDS]
        assert outputs[0]==outputs[1],code
    def mutate(name,fn):
        data=copy.deepcopy(original[name]);fn(data);write(root/'fixtures'/name,data)
    check('duplicate-scope',lambda:mutate('imports.json',lambda x:x['imports'].append(x['imports'][0])))
    check('digest-mismatch',lambda:mutate('imports.json',lambda x:x['imports'][0].update(sha256='0'*64)))
    check('wrong-kind',lambda:mutate('imports.json',lambda x:x['imports'][0].update(kind='verification-plan')))
    check('unsupported-format',lambda:mutate('current.json',lambda x:x.update(format='unknown')))
    check('incomplete-import',lambda:mutate('current.json',lambda x:x.update(complete=False)))
    check('invalid-artifact',lambda:mutate('current.json',lambda x:x['requirements'][0]['values'].pop('title')))
    check('invalid-artifact',lambda:mutate('current.json',lambda x:x['requirements'][0]['values'].update(statement=[{'kind':'unknown'}])))
    check('invalid-artifact',lambda:mutate('current.json',lambda x:x['requirements'].append(x['requirements'][0])))
    check('invalid-artifact',lambda:mutate('current.json',lambda x:x['requirements'][0]['locations']['record']['end'].update(line=0)))
    check('build-cycle',lambda:mutate('imports.json',lambda x:[x['imports'][0].update(dependsOn=['current']),x['imports'][1].update(dependsOn=['baseline'])]))
    check('missing-dependency',lambda:mutate('imports.json',lambda x:x['imports'][0].update(dependsOn=['absent'])))
    check('ambiguous-scope',lambda:mutate('plan.json',lambda x:x['plans'][0].update(currentScope=None)))
    check('missing-scope',lambda:mutate('plan.json',lambda x:x['plans'][0].update(currentScope='absent')))
    check('missing-target',lambda:mutate('plan.json',lambda x:x['coverage'][0].update(requirementId='ABSENT')))
    check('missing-activity',lambda:mutate('plan.json',lambda x:x['coverage'][0].update(activityId='ABSENT')))
    check('invalid-artifact',lambda:mutate('plan.json',lambda x:x['coverage'].append(x['coverage'][0])))
    check('invalid-json',lambda:(root/'fixtures/current.json').write_text('{"a":1,"a":2}'))
    check('input-unavailable',lambda:(root/'fixtures/current.json').unlink(),2)
    reset()
    for command in COMMANDS:
        assert len(invoke(command,root,context='vaccine-store')[0]['edges'])==57
        invoke(command,root,context='other',status=1,code='unknown-context')
    # An unselected context retains its own authored references without poisoning a selected plan.
    mutate('plan.json',lambda x:[x['plans'].append(dict(x['plans'][0],id='OTHER',context='other')),
        x['coverage'].append(dict(x['coverage'][0],planId='OTHER',requirementId='ABSENT'))])
    for command in COMMANDS:
        invoke(command,root,context='vaccine-store')
        invoke(command,root,status=1,code='missing-target')
    # Legal source cycle is exercised by the prior compiler corpus; add a valid self-edge/location here.
    reset();current=copy.deepcopy(original['current.json']);r=current['requirements'][0];target=r['values']['id']
    r['values']['decomposes']=[target];r['locations']['references']={target:r['locations']['fields']['id'][0]};r['locations']['fields']['decomposes']=r['locations']['fields']['id']
    write(root/'fixtures/current.json',current)
    for command in COMMANDS:invoke(command,root)
    reset()
    for command in COMMANDS:
        args=command+['--root',str(root),'--plan',str(root/'fixtures/plan.json'),str(root/'fixtures/imports.json')]
        process=subprocess.Popen(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        process.stdout.read(64);process.stdout.close();assert process.wait(timeout=30)==2;assert process.stderr.read()
        process=subprocess.run(['bash','-c','exec "$@" 1>&-','closed']+args,capture_output=True,timeout=30);assert process.returncode==2
        wrong=subprocess.run(command+['--root',str(root),'--plan',str(ROOT/'README.md'),str(root/'fixtures/imports.json')],capture_output=True);assert wrong.returncode==2 and not wrong.stdout
    # Real local Git repositories: pinned detached source and an independently authored plan checkout.
    requirements=root/'requirements-repo';plan_repo=root/'plan-repo'
    shutil.copytree(ROOT/'examples/yaml/vaccine-monitoring',requirements/'source')
    shutil.copytree(FIXTURE/'source',plan_repo/'source')
    def git(repo,*args):return subprocess.check_output(['git','-c','core.hooksPath=/dev/null','-c','user.name=Fixture','-c','user.email=fixture@example.invalid','-C',str(repo),*args],stderr=subprocess.DEVNULL,text=True).strip()
    for repo in [requirements,plan_repo]:git(repo,'init','-q');git(repo,'add','.');git(repo,'commit','-qm','Synthetic verification fixture')
    revision=git(requirements,'rev-parse','HEAD');pinned=root/'pinned-checkout';git(requirements,'worktree','add','--detach',str(pinned),revision)
    compiler=ROOT/'build/maintained/mundanereq-compile'
    def compile_req(source,target):
        with target.open('wb') as output:subprocess.run([str(compiler),'--source=yaml-0.3','--root',str(source),str(source)],stdout=output,check=True)
    compile_req(pinned/'source',root/'pinned.json')
    file=requirements/'source/system.mreq.yaml';file.write_text(file.read_text().replace('at least five years','at least six years'))
    git(requirements,'add','.');git(requirements,'commit','-qm','Synthetic changed source')
    compile_req(requirements/'source',root/'current.json');shutil.copyfile(FIXTURE/'fixtures/plan.json',plan_repo/'plan.json')
    manifest=copy.deepcopy(original['imports.json']);manifest['imports'][0].update(path='pinned.json',sha256=hashlib.sha256((root/'pinned.json').read_bytes()).hexdigest());manifest['imports'][1].update(path='current.json');write(root/'imports.json',manifest)
    for command in COMMANDS:
        result,_=invoke(command,root,'imports.json','plan-repo/plan.json');assert len(result['edges'])==57
        assert result['imports'][0]['sha256']==manifest['imports'][0]['sha256']
print('PASS linker: 57 source-linked edges; 18 invalid cases; qualification, contexts, cycles, actual two-repository pinned/current imports, parser-free JVM/native parity and output failures')
