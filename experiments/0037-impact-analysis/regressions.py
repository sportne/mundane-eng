"""Seeded reachability properties and adversarial public serialized boundaries."""
import copy
import json
import random
import shutil
import subprocess
import tempfile
import urllib.parse
import re
from pathlib import Path
import sys
sys.dont_write_bytecode = True
from workflow import BIN, ROOT, OUT, invoke, run, selection, verify, write


def main():
    verify()
    with tempfile.TemporaryDirectory(prefix='impact-regressions-') as temp:
        root = Path(temp)
        for file in ['req.json', 'baseline.json', 'plan.json', 'work.json', 'imports.json', 'impact.json']:
            shutil.copyfile(OUT / file, root / file)
        args = ['query', '--root', '.', '--from', 'req:requirement:SYS-001', 'imports.json']
        selected = json.loads((root / 'imports.json').read_text())
        good = invoke('mundane-impact', args, root)
        # Consumers can operate with no authoring source or source parsers on their classpath.
        isolated = root / 'classes'
        shutil.copytree(BIN / 'classes', isolated)
        for p in (isolated / 'mundanereq').rglob('*.class'):
            if p.name != 'Versions.class':
                p.unlink()
        for glob in ['WorkCompiler*.class', 'WorkYaml*.class', 'WorkMain*.class']:
            for p in (isolated / 'engineering/work').glob(glob):
                p.unlink()
        for p in (isolated / 'engineering/verification').glob('PlanCompiler*.class'):
            p.unlink()
        command = ['java', '-cp', str(isolated), 'engineering.impact.ImpactMain']
        assert run(command, args, root) == good
        assert run(command, ['view', '--root', '.', 'impact.json'], root) == (OUT / 'IMPACT.md').read_bytes()
        # Literal option-looking filenames are consumed after the delimiter in the actual cwd.
        for name in ['--help', '--version', '--']:
            (root / name).write_bytes((root / 'imports.json').read_bytes())
            literal = json.loads(invoke('mundane-impact', args[:-1] + ['--', name], root))
            assert literal['query'] == json.loads(good)['query']
        # Missing selected files, bad pin, scope conflict and unsupported format fail as a whole.
        mutations = [lambda m: m['imports'][0].update(sha256='0'*64),
                     lambda m: m['imports'][0].update(scope='baseline'),
                     lambda m: m['imports'][0].update(kind='work-items'),
                     lambda m: m.update(format='future'),
                     lambda m: m['imports'][0].update(dependsOn=['absent'])]
        for mutate in mutations:
            m = copy.deepcopy(selected); mutate(m); write(root / 'imports.json', m)
            a = json.loads(invoke('mundane-impact', args, root, 1))
            assert not a['complete'] and a['query'] is None and not a['edges'] and not a['imports']
        m = copy.deepcopy(selected); m['imports'][0]['path'] = 'missing.json'; write(root / 'imports.json', m)
        assert json.loads(invoke('mundane-impact', args, root, 2))['diagnostics'][0]['code'] == 'input-unavailable'
        # Exact pins must not conceal malformed attribute values when an updated pin is supplied.
        m = copy.deepcopy(selected); m['imports'][0]['sha256'] = None; write(root / 'imports.json', m)
        a = json.loads((OUT / 'req.json').read_text()); a['requirements'][0]['values']['attributes']['discipline'] = 'not-declared'
        write(root / 'req.json', a); invoke('mundane-impact', args, root, 1)
        # Independent fixed-point distance oracle over generated cyclic requirement graphs.
        template = json.loads((OUT / 'req.json').read_text())
        record = template['requirements'][0]
        rng = random.Random(1705)
        for trial in range(16):
            size = rng.randint(5, 24)
            parents = [sorted({rng.randrange(size) for _ in range(rng.randrange(5))}) for _ in range(size)]
            artifact = copy.deepcopy(template); records = []
            for i in range(size):
                r = copy.deepcopy(record); r['values'].update(id=f'R{i}', decomposes=[f'R{p}' for p in parents[i]])
                span = r['locations']['record']; r['locations']['references'] = {f'R{p}': copy.deepcopy(span) for p in parents[i]}
                r['locations']['fields'].pop('decomposes', None)
                if parents[i]:
                    r['locations']['fields']['decomposes'] = [copy.deepcopy(span)]
                records.append(r)
            artifact['requirements'] = records; write(root / 'req.json', artifact)
            selection(root, [('req', 'req.json', 'requirements')])
            start = rng.randrange(size); depth = rng.randint(1, 6)
            distances = {start: 0}
            for distance in range(1, size + 1):
                additions = {i: distance for i in range(size) if i not in distances and any(p in distances and distances[p] < distance for p in parents[i])}
                if not additions: break
                distances.update(additions)
            queryargs = ['query', '--root', '.', '--from', f'req:requirement:R{start}', '--depth', str(depth), 'imports.json']
            result = json.loads(invoke('mundane-impact', queryargs, root))
            actual = {int(r['node'].rsplit(':R', 1)[1]): len(r['path']) for r in result['query']['affected']}
            assert actual == {i: d for i, d in distances.items() if i != start and d <= depth}
            assert result['query']['truncated'] == any(d > depth for d in distances.values())
            for item in result['query']['affected']:
                previous = f'req:requirement:R{start}'
                for edge in item['path']:
                    assert edge['from'] == previous and edge['relation'] == 'decomposes'
                    parent, child = [int(edge[f].rsplit(':R', 1)[1]) for f in ['from', 'to']]
                    assert parent in parents[child]
                    previous = edge['to']
                assert previous == item['node']
            rng.shuffle(artifact['requirements']); write(root / 'req.json', artifact)
            selection(root, [('req', 'req.json', 'requirements')])
            again = json.loads(invoke('mundane-impact', queryargs, root))
            assert again['query'] == result['query'] and again['nodes'] == result['nodes'] and again['edges'] == result['edges']
        # Node limit is a failure, never a plausible partial graph.
        artifact = copy.deepcopy(template); artifact['requirements'] = []
        for i in range(10001):
            r = copy.deepcopy(record); r['values'].update(id=f'R{i}', decomposes=[])
            r['locations']['references'] = {}; r['locations']['fields'].pop('decomposes', None)
            artifact['requirements'].append(r)
        write(root / 'req.json', artifact); selection(root, [('req', 'req.json', 'requirements')])
        limited = json.loads(invoke('mundane-impact', ['query', '--root', '.', '--from', 'req:requirement:R0', 'imports.json'], root, 1))
        assert limited['diagnostics'][0]['code'] == 'impact-limit' and not limited['nodes']
        # An actual closed OS pipe is distinct from Java's PrintStream tests.
        process = subprocess.Popen([str(BIN / 'mundane-impact'), *args], cwd=OUT, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        process.stdout.close()
        errors = process.stderr.read(); process.stderr.close()
        assert process.wait(timeout=30) == 2 and b'output-failed' in errors
    # Every source link in the self-contained staged report resolves to the compiled source snapshot.
    for dest in re.findall(r'\]\(([^)]+)\)', (OUT / 'IMPACT.md').read_text()):
        file, line = dest.split('#L')
        source = OUT / urllib.parse.unquote(file)
        assert source.is_file() and 1 <= int(line) <= len(source.read_text().splitlines())
    print('PASS impact regressions: parser-free offline consumption, malformed/pinned imports, attributes, 16 seeded cyclic graphs, shortest paths, permutations, bounds, real broken pipe and source links')


if __name__ == '__main__':
    main()
