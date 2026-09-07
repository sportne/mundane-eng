"""Native safety traceability and exact review subjects, using the accepted failure ledger."""
import copy
import importlib.util
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station';BIN=ROOT/'build/maintained/mundane-safety';yaml=YAML(typ='safe')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def stage(root):
    shutil.copytree(ROOT/'build/gcs-configuration',root,dirs_exist_ok=True)
    shutil.copy2(HERE/'design/safety.yaml',root/'safety.yaml');shutil.copy2(ROOT/'build/gcs-seed/plan.json',root/'plan.json')
    imports=[dict(scope=scope,kind=kind,format=json.loads((root/file).read_text())['format'],path=file,sha256=sha(root/file)) for scope,kind,file in [('gcs-req','requirements','requirements.json'),('gcs-arch','architecture','architecture.json'),('gcs-config','configuration','configuration-sim.json'),('gcs-plan','verification-plan','plan.json')]]
    (root/'safety-imports.json').write_text(json.dumps(dict(format='mundane-domain-imports-0.1',imports=imports)))
def run(root,*args,code=0,command=None):
    p=subprocess.run([*(command or [str(BIN)]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(args,p.returncode,p.stderr.decode());return p.stdout

def verify():
    out=ROOT/'build/gcs-safety'
    if out.exists():shutil.rmtree(out)
    stage(out);compiled=run(out,'compile','--imports','safety-imports.json','safety.yaml');(out/'safety.json').write_bytes(compiled)
    (out/'safety.md').write_bytes(run(out,'view','safety.json'))
    query=json.loads(run(out,'query','safety.json','HZ-STALE'));assert query['controls'][0]['requirements'][0]['id']=='GCS-STALE'
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','safety'],cwd=ROOT,text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'check','safety.json',command=['java','-cp',cp,'engineering.safety.SafetyMain'])==compiled
    with tempfile.TemporaryDirectory(prefix='safety-workflow-') as tmp:
        root=Path(tmp);stage(root);assert run(root,'compile','--imports','safety-imports.json','safety.yaml')==compiled
        source=yaml.load(root/'safety.yaml')
        for case in yaml.load(HERE/'design/safety-cases.yaml')['cases']:
            d=copy.deepcopy(source)
            for change in case['changes']:
                v=d
                for part in change['path'][:-1]:v=v[part]
                v[change['path'][-1]]=change['value']
            yaml.dump(d,root/'changed.yaml')
            data=run(root,'compile','--imports','safety-imports.json','changed.yaml',code=1 if case['expected']=='rejected' else 0)
            if case['expected']!='rejected':
                (root/'changed.json').write_bytes(data);findings=json.loads(run(root,'analyze','changed.json'))['findings'];assert case['expected'] in {f['code'] for f in findings}
        # Change selected applicability while retaining the old reviewed subject.
        d=copy.deepcopy(source);d['reviewedAgainst'].append(dict(scope='gcs-config',sha256=sha(root/'configuration-sim.json')));d['context']['configuration']['id']='BL-FIELD-DESIGN'
        selection=json.loads((root/'safety-imports.json').read_text())
        for entry in selection['imports']:
            if entry['scope']=='gcs-config':entry['path']='configuration-field.json';entry['sha256']=sha(root/'configuration-field.json')
        (root/'changed-imports.json').write_text(json.dumps(selection));yaml.dump(d,root/'changed.yaml')
        (root/'changed.json').write_bytes(run(root,'compile','--imports','changed-imports.json','changed.yaml'))
        assert 'review-stale' in {f['code'] for f in json.loads(run(root,'analyze','changed.json'))['findings']}
    print('PASS native safety compile/check/analyze/query/view, source-linked FMEA/tree, ten cases, configuration review drift, deterministic rebuild and YAML-free consumer')
if __name__=='__main__':verify()
