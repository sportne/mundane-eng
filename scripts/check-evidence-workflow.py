"""Native executed and imported evidence, exact provenance, and adversarial result queries."""
import copy
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station';BIN=ROOT/'build/maintained';yaml=YAML(typ='safe');yaml.allow_duplicate_keys=False
NAMES=['stale','combined','inspection']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,d):p.write_text(json.dumps(d,sort_keys=True,separators=(',',':'))+'\n')
def run(root,tool,*args,code=0,command=None):
    p=subprocess.run([*(command or [str(BIN/tool)]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(tool,args,p.returncode,p.stderr.decode());return p.stdout

def stage(root):
    shutil.copytree(ROOT/'build/gcs-configuration',root,dirs_exist_ok=True)
    shutil.copy2(ROOT/'build/gcs-seed/plan.json',root/'plan.json')
    entries=[dict(scope=scope,kind=kind,format=json.loads((root/file).read_text())['format'],path=file,sha256=sha(root/file)) for scope,kind,file in [('gcs-req','requirements','requirements.json'),('gcs-arch','architecture','architecture.json'),('gcs-config','configuration','configuration-sim.json'),('gcs-plan','verification-plan','plan.json')]]
    write(root/'procedure-imports.json',dict(format='mundane-domain-imports-0.1',imports=entries))
    for name in NAMES:
        shutil.copy2(HERE/'design'/f'procedure-{name}.yaml',root/f'procedure-{name}.yaml')
        (root/f'procedure-{name}.json').write_bytes(run(root,'mundane-procedure','compile','--imports','procedure-imports.json',f'procedure-{name}.yaml'))
    (root/'runtime').mkdir(exist_ok=True);shutil.copy2(BIN/'mundane-evidence',root/'runtime/mundane-evidence')
    if (HERE/'engineering/manual-inspection.yaml').exists():shutil.copy2(HERE/'engineering/manual-inspection.yaml',root/'manual-inspection.yaml')

def import_run(root,procedure,raw,name):
    path=root/f'{name}.json';path.write_bytes(run(root,'mundane-evidence','import',procedure,raw));return path

def analyze(root,procedure,*evidence):return json.loads(run(root,'mundane-evidence','analyze',procedure,*evidence))

def generate(root):
    stage(root)
    for name in ['stale','combined']:
        (root/f'run-{name}.json').write_bytes(run(root,'mundane-evidence','simulate',f'procedure-{name}.json','runtime/mundane-evidence','nominal'))
        import_run(root,f'procedure-{name}.json',f'run-{name}.json',f'evidence-{name}')
        assert analyze(root,f'procedure-{name}.json',f'evidence-{name}.json')['observedSupport']
    (root/'run-manual.json').write_bytes(run(root,'mundane-evidence','normalize-manual','manual-inspection.yaml','runtime/mundane-evidence'))
    import_run(root,'procedure-inspection.json','run-manual.json','evidence-manual')
    assert analyze(root,'procedure-inspection.json','evidence-manual.json')['observedSupport']
    assessment=dict(format='mundane-assessment-yaml-0.1',id='ASSESS-SYNTHETIC',run=dict(id=json.loads((root/'run-manual.json').read_text())['id'],sha256=sha(root/'run-manual.json')),disposition='disputed',reviewer='synthetic-reviewer',role='verification',reason='Fixture demonstrates a disputed assessment; no physical wiring was inspected.')
    yaml.dump(assessment,root/'assessment.yaml');(root/'assessment.json').write_bytes(run(root,'mundane-evidence','assess','assessment.yaml'))
    result=analyze(root,'procedure-inspection.json','evidence-manual.json','assessment.json');assert not result['adequacyEstablished'] and result['assessments'][0]['subjectState']=='matched'
    return {p.name:p.read_bytes() for p in root.glob('procedure-*.json')}

def verify():
    out=ROOT/'build/gcs-evidence'
    if out.exists():shutil.rmtree(out)
    expected=generate(out)
    (out/'evidence.md').write_bytes(run(out,'mundane-evidence','view','evidence-stale.json'))
    for name in ['stale','combined','manual']:
        schema=json.loads((ROOT/'specification/schema/run-0.1.json').read_text());Draft202012Validator(schema).validate(json.loads((out/f'run-{name}.json').read_text()))
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','evidence'],cwd=ROOT,text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'mundane-evidence','check','evidence-stale.json',command=['java','-cp',cp,'engineering.evidence.EvidenceMain'])==(out/'evidence-stale.json').read_bytes()
    with tempfile.TemporaryDirectory(prefix='evidence-rebuild-') as tmp:
        root=Path(tmp);assert generate(root)==expected
        for name in ['run-stale.json','run-combined.json','run-manual.json']:
            assert (root/name).read_bytes()==(out/name).read_bytes()
        (root/'run-defect.json').write_bytes(run(root,'mundane-evidence','simulate','procedure-stale.json','runtime/mundane-evidence','suppress-stale'))
        import_run(root,'procedure-stale.json','run-defect.json','evidence-defect')
        result=analyze(root,'procedure-stale.json','evidence-stale.json','evidence-defect.json');assert result['conflictingRuns'] and not result['observedSupport']
        assert not analyze(root,'procedure-stale.json')['observedSupport']
        raw=json.loads((root/'run-stale.json').read_text())
        for label,changes in [('wrong-procedure',{'procedure':dict(raw['procedure'],sha256='0'*64)}),('wrong-subject',{'subjects':[]}),('wrong-build',{'adapter':dict(raw['adapter'],buildSha256='0'*64)}),('wrong-runtime',{'runtime':dict(raw['runtime'],sha256='0'*64)})]:
            write(root/'bad-run.json',dict(raw,**changes));run(root,'mundane-evidence','import','procedure-stale.json','bad-run.json',code=1)
        for state,changes in [('skipped',{'execution':'skipped'}),('interrupted',{'execution':'interrupted'}),('inconclusive',{'observations':[]}),('inconclusive',{'clock':dict(raw['clock'],uncertaintyMs=999)})]:
            write(root/'outcome-run.json',dict(raw,**changes));import_run(root,'procedure-stale.json','outcome-run.json','outcome-evidence')
            result=analyze(root,'procedure-stale.json','outcome-evidence.json');assert not result['observedSupport'] and result['runs'][0]['outcome']['state']==state
        too_many=yaml.load(root/'procedure-stale.yaml');too_many['events']=[dict(atMs=0,kind='heartbeat') for _ in range(1001)];yaml.dump(too_many,root/'too-many.yaml')
        run(root,'mundane-procedure','compile','--imports','procedure-imports.json','too-many.yaml',code=1)
        # A new procedure revision cannot inherit the old run, even with the same human ID.
        p=yaml.load(root/'procedure-stale.yaml');p['objective']+=' Revised review subject.';yaml.dump(p,root/'changed-procedure.yaml')
        (root/'changed-procedure.json').write_bytes(run(root,'mundane-procedure','compile','--imports','procedure-imports.json','changed-procedure.yaml'))
        result=analyze(root,'changed-procedure.json','evidence-stale.json');assert not result['observedSupport'] and result['runs'][0]['outcome']['state']=='stale'
        # Stale assessment subjects are explicit; no trusted identity is inferred.
        a=yaml.load(root/'assessment.yaml');a['run']['sha256']='0'*64;yaml.dump(a,root/'stale-assessment.yaml');(root/'stale-assessment.json').write_bytes(run(root,'mundane-evidence','assess','stale-assessment.yaml'))
        result=analyze(root,'procedure-inspection.json','evidence-manual.json','stale-assessment.json');assert result['assessments'][0]['subjectState']=='unavailable-or-stale'
        # Stored verdict fields cannot override the derived evaluation.
        forged=json.loads((root/'evidence-stale.json').read_text());forged['values']['state']='pass';write(root/'forged.json',forged);run(root,'mundane-evidence','check','forged.json',code=1)
        (root/'run-stale.json').write_bytes((root/'run-stale.json').read_bytes()+b' ');run(root,'mundane-evidence','check','evidence-stale.json',code=1)
        (root/'run-stale.json').unlink();run(root,'mundane-evidence','check','evidence-stale.json',code=2)
        (root/'manual-inspection.yaml').write_text('changed manual observation\n');run(root,'mundane-evidence','check','evidence-manual.json',code=1)
    print('PASS native procedure/evidence: real deterministic simulations and injected defect, manual adapter and assessments, clean rebuild, exact pins/runtime/raw resources, failures/conflicts/staleness and YAML-free consumers')
if __name__=='__main__':verify()
