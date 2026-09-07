"""Maintained architecture public commands against the GCS seed, including isolated consumers."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
BIN=ROOT/'build/maintained/mundane-architecture'
def run(root,*args,code=0,command=None):
    result=subprocess.run([*(command or [str(BIN)]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert result.returncode==code,(args,result.returncode,result.stderr.decode())
    return result.stdout

def stage(root):
    shutil.copy2(ROOT/'examples/ground-control-station/design/architecture.yaml',root/'architecture.yaml')
    shutil.copytree(ROOT/'build/gcs-seed/source',root/'source')
    shutil.copy2(ROOT/'build/gcs-seed/current.json',root/'requirements.json')
    entries=[dict(scope='gcs-req',kind='requirements',format='mundanereq-requirements-0.2',path='requirements.json',sha256=hashlib.sha256((root/'requirements.json').read_bytes()).hexdigest())]
    (root/'imports.json').write_text(json.dumps(dict(format='mundane-domain-imports-0.1',imports=entries)))

def verify():
    out=ROOT/'build/gcs-architecture'
    if out.exists():shutil.rmtree(out)
    out.mkdir(parents=True);stage(out)
    compiled=run(out,'compile','--imports','imports.json','architecture.yaml');(out/'architecture.json').write_bytes(compiled)
    view=run(out,'view','architecture.json');(out/'architecture.md').write_bytes(view)
    assert b'IF-TELEMETRY' in view and b'#L' in view and b'flowchart LR' in view
    assert run(out,'view','architecture.json')==view
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','architecture'],cwd=ROOT,text=True).strip()
    cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'view','architecture.json',command=['java','-cp',cp,'engineering.architecture.ArchitectureMain'])==view
    with tempfile.TemporaryDirectory(prefix='architecture-rebuild-') as tmp:
        clean=Path(tmp);stage(clean);assert run(clean,'compile','--imports','imports.json','architecture.yaml')==compiled
        source=clean/'architecture.yaml';source.write_text(source.read_text().replace('value: 500','value: 400'))
        changed=run(clean,'compile','--imports','imports.json','architecture.yaml');assert changed!=compiled
        (clean/'architecture.json').write_bytes(changed);assert b'400' in run(clean,'view','architecture.json')
        (clean/'requirements.json').write_bytes((clean/'requirements.json').read_bytes()+b' ')
        assert b'digest-mismatch' in subprocess.run([str(BIN),'check','--root',str(clean),'architecture.json'],capture_output=True).stderr
    print('PASS native architecture compile/check/view, clean deterministic rebuild, freshness edit, pinned imports and YAML-free consumer')
if __name__=='__main__':verify()
