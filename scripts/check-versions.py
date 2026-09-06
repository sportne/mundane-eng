"""Check actual command metadata and mutation detection without changing the tree."""
import importlib.util
import json
import subprocess
import tempfile
import sys
from pathlib import Path

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('generator', ROOT/'scripts/generate-versions.py')
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)
values = generator.read(ROOT/'versions.properties')
metadata = json.loads((ROOT/'build/maintained/generated/versions.json').read_text())
def check_metadata(actual):
    if values != actual:
        raise ValueError('generated package metadata disagrees with declarations')
check_metadata(metadata)
if len(sys.argv) > 1:
    assert sys.argv[1] == values['SUITE_VERSION'], 'Make package version overrides authoritative declaration'
cp = str(ROOT/'build/maintained/classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
for tool, main in [('VALIDATE','Validator'),('FORMAT','Formatter'),('TRACE','Trace'),('COMPILE','Compile')]:
    actual = subprocess.check_output(['java','-cp',cp,'mundanereq.cli.'+main+'Main','--version'], text=True)
    assert values[tool+'_VERSION'] in actual and values['SOURCE_YAML'] in actual
    yaml = subprocess.check_output(['java','-cp',cp,'mundanereq.cli.'+main+'Main','--source=yaml-0.3','--version'], text=True)
    assert values['SOURCE_YAML'] in yaml
for tool, main, formats in [('WORK','engineering.work.WorkMain',['WORK_SOURCE','WORK_ARTIFACT','WORK_ANALYSIS']),('LINK','engineering.artifacts.LinkMain',['IMPORT_FORMAT','LINK_ARTIFACT']),('PLAN','engineering.verification.PlanMain',['PLAN_SOURCE','PLAN_ARTIFACT']),('VERIFY','engineering.verification.VerifyMain',['VERIFICATION_ARTIFACT'])]:
    actual=subprocess.check_output(['java','-cp',cp,main,'--version'],text=True)
    assert all(values[key] in actual for key in [tool+'_VERSION',tool+'_CONTRACT']+formats)
# The same agreement predicate catches stale metadata, not just copied constants.
stale = dict(metadata, SUITE_VERSION='deliberate-disagreement')
try: check_metadata(stale)
except ValueError: pass
else: raise AssertionError('stale metadata was accepted')
with tempfile.TemporaryDirectory() as directory:
    changed = dict(values, VALIDATE_VERSION='experimental-change')
    generator.generate(changed, directory)
    result = json.loads((Path(directory)/'versions.json').read_text())
    assert result['VALIDATE_VERSION'] != values['VALIDATE_VERSION']
    assert all(result[k] == v for k,v in values.items() if k != 'VALIDATE_VERSION')
    java = Path(directory)/'mundanereq/Versions.java'
    subprocess.run(['javac','--release','21',str(java)],check=True)
    bad=Path(directory)/'bad.properties';bad.write_text('SUITE_VERSION=one\nSUITE_VERSION=two\n')
    try: generator.read(bad)
    except ValueError: pass
    else: raise AssertionError('duplicate declaration accepted')
print('PASS independent version declarations, actual CLI metadata, stale metadata and isolated version-domain mutation')

sarif=json.loads(subprocess.check_output(['java','-cp',cp,'mundanereq.cli.ValidatorMain','--output=sarif','--root','conformance/0.3/valid','conformance/0.3/valid'],cwd=ROOT))
assert sarif['version']==values['SARIF_VERSION']
assert sarif['runs'][0]['properties']['commandContract']==values['VALIDATE_CONTRACT']
print('PASS SARIF format and validator command metadata consume authoritative declarations')

editor_spec = importlib.util.spec_from_file_location('editor_versions', ROOT/'scripts/editor-versions.py')
editor = importlib.util.module_from_spec(editor_spec)
editor_spec.loader.exec_module(editor)
expected = editor.synchronize(ROOT, values)
actual = json.loads(subprocess.check_output(['java','-cp',cp,'mundanereq.editor.EditorMain','--version']))
assert actual == expected
with tempfile.TemporaryDirectory() as tmp:
    sandbox = Path(tmp); target = sandbox/'editors/vscode'; target.mkdir(parents=True)
    for name in ['package.json','package-lock.json','versions.json']:
        (target/name).write_bytes((ROOT/'editors/vscode'/name).read_bytes())
    changed = dict(values, EDITOR_VERSION='0.1.99', EDITOR_PROTOCOL='mundane-editor-test-0.9')
    try: editor.synchronize(sandbox, changed)
    except ValueError: pass
    else: raise AssertionError('stale editor metadata accepted')
    editor.synchronize(sandbox, changed, True)
    assert editor.synchronize(sandbox, changed) == editor.expected(changed)
    assert json.loads((target/'package-lock.json').read_text())['packages']['']['version'] == '0.1.99'
print('PASS actual JVM editor metadata, stale editor metadata rejection and isolated version/protocol regeneration')
