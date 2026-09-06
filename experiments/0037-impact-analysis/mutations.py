"""Kill four local behavioral mutations through the public impact command."""
import json
import subprocess
import tempfile
from pathlib import Path
import sys
sys.dont_write_bytecode = True
from workflow import BIN, ROOT, OUT, run, write


def check(command):
    args = ['query', '--root', '.', '--from', 'req:requirement:SYS-001', 'imports.json']
    actual = run(command, args)
    assert actual == (OUT / 'impact.json').read_bytes(), 'changed impact policy'
    bounded = json.loads(run(command, args[:-1] + ['--depth', '1', 'imports.json']))
    assert bounded['query']['truncated'] and len(bounded['query']['affected']) == 1, 'ignored depth'
    with tempfile.TemporaryDirectory(prefix='impact-mutation-inputs-') as temp:
        root = Path(temp)
        m = json.loads((OUT / 'imports.json').read_text())
        for entry in m['imports']:
            (root / entry['path']).write_bytes((OUT / entry['path']).read_bytes())
        m['imports'][0]['sha256'] = '0'*64; write(root / 'imports.json', m)
        run(command, args, root, 1)
        a = json.loads((OUT / 'impact.json').read_text()); a['edges'] = []; write(root / 'query.json', a)
        assert not run(command, ['view', '--root', '.', 'query.json'], root, 1), 'forged report rendered'


mutations = [
    ('ImpactGraph', 'Set.of("addresses", "depends-on")', 'Set.of("relates-to", "depends-on")'),
    ('ImpactQuery', 'if (distance == depth) continue;', 'if (false) continue;'),
    ('ImpactInputs', 'if (entry.get("sha256") != null &&', 'if (false && entry.get("sha256") != null &&'),
    ('ImpactView', 'if (!same(graph.nodes(), output.get("nodes")) || !same(graph.edges(), output.get("edges")) || !same(expected, query))', 'if (false)'),
]

if __name__ == '__main__':
    base = ['java', '-cp', str(BIN / 'classes'), 'engineering.impact.ImpactMain']
    check(base)
    for name, original, changed in mutations:
        source = (ROOT / 'src/main/java/engineering/impact' / (name + '.java')).read_text()
        assert source.count(original) == 1, (name, 'mutation anchor changed')
        with tempfile.TemporaryDirectory(prefix='impact-mutant-') as temp:
            root = Path(temp); file = root / (name + '.java'); file.write_text(source.replace(original, changed))
            subprocess.run(['javac', '--release', '21', '-cp', str(BIN / 'classes'), '-d', str(root / 'classes'), str(file)], check=True, capture_output=True)
            command = ['java', '-cp', str(root / 'classes') + ':' + str(BIN / 'classes'), 'engineering.impact.ImpactMain']
            try:
                check(command)
            except AssertionError:
                print('PASS killed impact mutation:', name)
            else:
                raise AssertionError('surviving mutation: ' + name)
    print('PASS all four behavioral mutations detected; production files unchanged')
