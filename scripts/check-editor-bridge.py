"""Exercise serialized editor requests through JVM and native process boundaries."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BIN = ROOT / 'build/maintained'
CP = str(BIN / 'classes') + ':' + str(ROOT / 'build/dependencies/snakeyaml-engine-3.1.1.jar')
commands = [[str(BIN / 'mundane-editor')], ['java', '-cp', CP, 'mundanereq.editor.EditorMain']]
expected = json.loads((ROOT / 'editors/vscode/versions.json').read_text())
for command in commands:
    assert json.loads(subprocess.check_output(command + ['--version'])) == expected
fixtures = ROOT / 'editors/vscode/test/fixtures'
request = {'protocol': 'mundane-editor-0.1', 'source': 'yaml-0.3',
           'files': [{'path': name, 'text': (fixtures / name).read_text()}
                     for name in ['parent.mreq.yaml', 'child.mreq.yaml']], 'schema': None}

for case in [request, dict(request, files=[request['files'][1]]),
             dict(request, protocol='unknown'), dict(request, files=[]),
             dict(request, cursor={'path': 'missing.yaml', 'line': 1, 'column': 1})]:
    results = [subprocess.run(command, input=(json.dumps(case) + '\n').encode(),
                              capture_output=True, timeout=30) for command in commands]
    assert [(p.returncode, p.stdout, p.stderr) for p in results][0] == [(p.returncode, p.stdout, p.stderr) for p in results][1]
    if case is request:
        result = json.loads(results[0].stdout)
        assert result['valid'] and len(result['definitions']) == 2
    elif case.get('files') == [request['files'][1]]:
        assert results[0].returncode == 0 and not json.loads(results[0].stdout)['valid']
    else:
        assert results[0].returncode == 2 and not results[0].stdout
for command in commands:
    with open('/dev/full', 'wb') as unavailable:
        failed = subprocess.run(command, input=json.dumps(request).encode(), stdout=unavailable,
                                stderr=subprocess.PIPE, timeout=30)
        assert failed.returncode == 2 and b'output unavailable' in failed.stderr
print('PASS editor JVM/native request parity, incomplete semantics, rejected protocol/cursor/selection and failed output delivery')

selection = json.loads((ROOT / 'roadmap/work-items.json').read_text())
work = {'protocol': expected['protocol'], 'source': 'mundane-work-yaml-0.2', 'schema': None,
        'files': [{'path': name, 'text': (ROOT / name).read_text()} for name in selection['files']]}
for case in [work, dict(work, schema={'path':'schema.yaml','text':'{}'}), dict(work, files=[]),
             dict(work, cursor={'path':work['files'][0]['path'], 'line':6, 'column':10})]:
    results = [subprocess.run(command, input=json.dumps(case).encode(), capture_output=True, timeout=30) for command in commands]
    assert (results[0].returncode,results[0].stdout,results[0].stderr) == (results[1].returncode,results[1].stdout,results[1].stderr)
    if case.get('schema') is not None or not case['files']:
        assert results[0].returncode == 2 and not results[0].stdout
    else:
        result=json.loads(results[0].stdout)
        assert result['valid'] and len(result['definitions']) == len(work['files'])
for command in commands:
    with open('/dev/full','wb') as unavailable:
        result=subprocess.run(command,input=json.dumps(work).encode(),stdout=unavailable,stderr=subprocess.PIPE,timeout=30)
        assert result.returncode == 2 and b'output unavailable' in result.stderr
print('PASS work editor JVM/native parity, full repository backlog, invalid requests and output failure')
