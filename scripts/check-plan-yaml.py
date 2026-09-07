"""Public YAML plan compiler parity and independent decoded-value/provenance checks."""
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path
import source_yaml

ROOT = Path(__file__).resolve().parents[1]
CP = str(ROOT/'build/maintained/classes') + ':' + str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
COMMANDS = [[str(ROOT/'build/maintained/mundane-plan')], ['java', '-cp', CP, 'engineering.verification.PlanMain']]


def invoke(root, directory, status=0):
    results = [subprocess.run(c + ['--root', str(root), str(directory)], capture_output=True, timeout=30) for c in COMMANDS]
    for r in results:
        assert r.returncode == status, (r.returncode, r.stdout, r.stderr)
    assert results[0].stdout == results[1].stdout and results[0].stderr == results[1].stderr
    return json.loads(results[0].stdout)


for relative in ['examples/attributes/plan', 'examples/impact/plan', 'experiments/0028-verification-contract/source']:
    directory = ROOT/relative
    source = directory/'plan.yaml'
    expected = source_yaml.loads(source.read_text())
    result = invoke(ROOT, directory)
    assert result['complete'] and result['sourceContract'] == 'mundane-plan-yaml-0.1'
    assert result['sources'] == [{'path': source.relative_to(ROOT).as_posix(), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}]
    for field in ['plans', 'activities', 'coverage']:
        actual = [{k: v for k, v in row.items() if k != 'location'} for row in result[field]]
        rows = expected[field]
        if field == 'plans':
            rows = [dict(baselineScope=None, currentScope=None) | row for row in rows]
        assert sorted(map(lambda r: json.dumps(r, sort_keys=True), actual)) == sorted(map(lambda r: json.dumps(r, sort_keys=True), rows))
        for row in result[field]:
            point = row['location']
            line = source.read_text().splitlines()[point['line']-1]
            assert line[point['column']-1:].startswith('id:' if field != 'coverage' else 'planId:'), point
    assert invoke(ROOT, directory) == result

with tempfile.TemporaryDirectory(prefix='plan-yaml-public-') as folder:
    root = Path(folder)
    valid = (ROOT/'examples/attributes/plan/plan.yaml').read_text()
    for bad in [valid.replace('mundane-plan-yaml-0.1', 'mundane-plan-source-0.1'),
                valid + '---\n{}\n', valid.replace('method: review', 'method: [review]'),
                valid.replace('activityId: ACT-REVIEW', 'activityId: MISSING'),
                valid.replace('context: logger', 'context: logger\n  context: logger')]:
        (root/'plan.yaml').write_text(bad)
        result = invoke(root, root, 1)
        assert not result['complete'] and not result['plans'] and not result['activities'] and not result['coverage']
    (root/'plan.yaml').unlink()
    (root/'plan.tsv').write_text('format\tplan_id\n')
    assert not invoke(root, root, 2)['complete']
print('PASS YAML plans: public JVM/native parity, decoded semantics, exact provenance, deterministic ordering and old-source rejection')
