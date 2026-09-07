"""Compile the checked-in example and compare public query/report behavior to goldens."""
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BIN = ROOT / 'build/maintained'
OUT = ROOT / 'build/impact-example'
GOLD = Path(__file__).parent / 'golden'
CP = str(BIN / 'classes') + ':' + str(ROOT / 'build/dependencies/snakeyaml-engine-3.1.1.jar')
TOOLS = {'mundanereq-compile': 'mundanereq.cli.CompileMain',
         'mundane-plan': 'engineering.verification.PlanMain',
         'mundane-work': 'engineering.work.WorkMain',
         'mundane-impact': 'engineering.impact.ImpactMain'}


def run(command, args, root=OUT, status=0):
    p = subprocess.run(command + args, cwd=root, capture_output=True, timeout=45)
    assert p.returncode == status, (command, args, p.returncode, p.stderr, p.stdout[-1000:])
    if status == 0:
        assert not p.stderr, p.stderr
    return p.stdout


def invoke(tool, args, root=OUT, status=0):
    native = run([str(BIN / tool)], args, root, status)
    jvm = run(['java', '-cp', CP, TOOLS[tool]], args, root, status)
    assert native == jvm, (tool, 'JVM/native mismatch')
    return native


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')) + '\n')


def selection(root, scopes):
    imports = []
    for scope, file, kind in scopes:
        imports.append({'scope': scope, 'path': file, 'kind': kind,
                        'sha256': hashlib.sha256((root / file).read_bytes()).hexdigest(), 'dependsOn': []})
    write(root / 'imports.json', {'format': 'mundane-imports-0.1', 'imports': imports})


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    if (OUT / 'source').exists():
        shutil.rmtree(OUT / 'source')
    shutil.copytree(ROOT / 'examples/impact', OUT / 'source')
    before = {str(p.relative_to(OUT)): p.read_bytes() for p in (OUT / 'source').rglob('*') if p.is_file()}
    req = invoke('mundanereq-compile', ['--source=yaml-0.4', '--root', '.', '--attribute-schema', 'source/attributes.yaml', 'source/requirements.yaml'])
    (OUT / 'req.json').write_bytes(req)
    # An explicitly selected exact revision; scope identity remains distinct even with equal bytes.
    (OUT / 'baseline.json').write_bytes(req)
    (OUT / 'plan.json').write_bytes(invoke('mundane-plan', ['--root', '.', 'source/plan']))
    (OUT / 'work.json').write_bytes(invoke('mundane-work', ['compile', '--root', '.', 'source/work-items.json']))
    selection(OUT, [('req', 'req.json', 'requirements'), ('baseline', 'baseline.json', 'requirements'),
                    ('plan', 'plan.json', 'verification-plan'), ('work', 'work.json', 'work-items')])
    args = ['query', '--root', '.', '--from', 'req:requirement:SYS-001', 'imports.json']
    result = invoke('mundane-impact', args)
    (OUT / 'impact.json').write_bytes(result)
    a = json.loads(result)
    expected = ['plan:verification-activity:ACT-STORAGE', 'plan:verification-plan:PLAN-LOGGER',
                'req:requirement:DEV-001', 'work:work-item:TASK-PLAN',
                'work:work-item:TASK-PROCEDURE', 'work:work-item:TASK-STORAGE']
    assert [r['node'] for r in a['query']['affected']] == expected
    assert not a['query']['truncated']
    assert all(r['node'] != 'plan:verification-activity:ACT-POWER' for r in a['query']['affected'])
    report = invoke('mundane-impact', ['view', '--root', '.', 'impact.json'])
    (OUT / 'IMPACT.md').write_bytes(report)
    assert all((OUT / file).read_bytes() == content for file, content in before.items())
    # Explicitly supported old requirement output, with schema/attributes absent, has the same topology.
    old = json.loads(req)
    old.pop('attributeSchema')
    old.update(format='mundanereq-requirements-0.1', sourceContract='mundanereq-yaml-0.3')
    for record in old['requirements']:
        record['values'].pop('attributes')
        record['locations'].pop('attributes')
    write(OUT / 'yaml03.json', old)
    original = (OUT / 'imports.json').read_bytes()
    try:
        selection(OUT, [('req', 'yaml03.json', 'requirements'), ('baseline', 'yaml03.json', 'requirements'),
                        ('plan', 'plan.json', 'verification-plan'), ('work', 'work.json', 'work-items')])
        yaml03 = json.loads(invoke('mundane-impact', args))
        assert yaml03['query'] == a['query'] and yaml03['edges'] == a['edges']
    finally:
        (OUT / 'imports.json').write_bytes(original)
    return result, report


def verify():
    result, report = build()
    assert result == (GOLD / 'impact.json').read_bytes(), 'impact golden changed'
    assert report == (GOLD / 'impact-report.txt').read_bytes(), 'report golden changed'
    print('PASS impact workflow: actual source compilers, YAML 0.3/0.4 topology, six review candidates, isolated scopes, unchanged source/status, JVM/native and golden parity')


if __name__ == '__main__':
    verify()
