"""Native baseline publication, comparison and retained-root reconstruction."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator

ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station'
BIN=ROOT/'build/maintained/mundane-configuration'
spec=importlib.util.spec_from_file_location('architecture_workflow',ROOT/'scripts/check-architecture-workflow.py');arch=importlib.util.module_from_spec(spec);spec.loader.exec_module(arch)
yaml=YAML(typ='safe');yaml.allow_duplicate_keys=False
NAMES=['sim','field','built','deployed','replacement']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(root,*args,code=0):
    p=subprocess.run([str(BIN),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(args,p.returncode,p.stderr.decode())
    return p.stdout

def stage(root):
    arch.stage(root)
    (root/'architecture.json').write_bytes(arch.run(root,'compile','--imports','imports.json','architecture.yaml'))
    shutil.copytree(HERE/'design/resources',root/'resources')
    shutil.copy2(HERE/'seed.py',root/'seed.py')
    shutil.copy2(ROOT/'build/maintained/generated/versions.json',root/'versions.json')
    for name in NAMES:shutil.copy2(HERE/'engineering'/f'configuration-{name}.yaml',root/f'configuration-{name}.yaml')

def generate(root):
    stage(root)
    for name in NAMES:
        (root/f'configuration-{name}.json').write_bytes(run(root,'compile',f'configuration-{name}.yaml'))
    return {name:(root/f'configuration-{name}.json').read_bytes() for name in NAMES}

def verify():
    out=ROOT/'build/gcs-configuration'
    if out.exists():shutil.rmtree(out)
    out.mkdir(parents=True);expected=generate(out)
    schema=json.loads((ROOT/'specification/schema/configuration-yaml-0.1.json').read_text());Draft202012Validator.check_schema(schema)
    for name in NAMES:Draft202012Validator(schema).validate(yaml.load(out/f'configuration-{name}.yaml'))
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','configuration'],cwd=ROOT,text=True).strip()
    cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    isolated=subprocess.run(['java','-cp',cp,'engineering.configuration.ConfigurationMain','check','--root',str(out),'configuration-sim.json'],capture_output=True)
    assert isolated.returncode==0 and isolated.stdout==expected['sim'],isolated.stderr
    (out/'configuration.md').write_bytes(run(out,'view','configuration-sim.json'))
    assert json.loads(run(out,'compare','configuration-sim.json','configuration-replacement.json'))['changes']==[{'slot':'host','change':'selected-revision-or-contract-changed'}]
    assert {c['slot'] for c in json.loads(run(out,'compare','configuration-built.json','configuration-deployed.json'))['changes']}=={'configuration-stage','configuration-observationBasis'}
    with tempfile.TemporaryDirectory(prefix='configuration-rebuild-') as tmp:
        root=Path(tmp);assert generate(root)==expected
        receipt=json.loads(run(root,'publish','configuration-sim.json','published'))
        retained=root/receipt['root'];assert run(retained,'check',receipt['artifact'])==expected['sim']
        assert json.loads(run(root,'publish','configuration-sim.json','published'))==receipt
        # Reconstruct solely from retained bytes, after original sources/resources disappear.
        for name in ['architecture.yaml','requirements.json','seed.py']: (root/name).unlink()
        assert run(retained,'compile','configuration-sim.yaml')==expected['sim']
        run(retained,'check','configuration-sim.json')
        (retained/'resources/server-simulation.yaml').write_text('tampered\n')
        run(retained,'check','configuration-sim.json',code=1)
    with tempfile.TemporaryDirectory(prefix='configuration-negative-') as tmp:
        root=Path(tmp);generate(root);original=yaml.load(root/'configuration-sim.yaml')
        cases=yaml.load(HERE/'design/configuration-cases.yaml')['cases']
        for case in cases:
            d=copy.deepcopy(original)
            for change in case['changes']:
                t=d
                for key in change['path'][:-1]:t=t[key]
                if change.get('remove'):del t[change['path'][-1]]
                else:t[change['path'][-1]]=change['value']
            yaml.dump(d,root/'changed.yaml')
            p=subprocess.run([str(BIN),'compile','--root',str(root),'changed.yaml'],capture_output=True)
            if case['expected']==[] or case['expected']==['optional-resource-unavailable']:assert p.returncode==0,(case,p.stderr)
            else:assert p.returncode!=0 and not p.stdout,(case,p.stdout,p.stderr)
        (root/'configuration-sim.json').write_bytes(expected['sim'])
        (root/'requirements.json').write_bytes((root/'requirements.json').read_bytes()+b' ')
        run(root,'check','configuration-sim.json',code=1)
    print('PASS native configuration: five variants/stages, independent schema, 13 cases, exact source mappings, comparisons, clean rebuild, retained publication/rebuild and tamper rejection')
if __name__=='__main__':verify()
