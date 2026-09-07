"""Public requirement commands honor literal paths after --, including option-like names."""
import json
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BIN = ROOT / 'build/maintained'
CP = str(BIN / 'classes') + ':' + str(ROOT / 'build/dependencies/snakeyaml-engine-3.1.1.jar')
TOOLS = {'validate': 'Validator', 'format': 'Formatter', 'trace': 'Trace', 'compile': 'Compile'}
with tempfile.TemporaryDirectory(prefix='literal-inputs-') as temporary:
    root = Path(temporary)
    (root / 'schema.yaml').write_bytes((ROOT / 'examples/attributes/requirement-attributes.yaml').read_bytes())
    original = (ROOT / 'examples/attributes/system.mreq.yaml').read_bytes()
    for filename in ['--help', '--version', '--attribute-schema', '--']:
        (root / filename).write_bytes(original)
        for tool, main in TOOLS.items():
            mode = {'validate': [], 'format': ['--check'], 'trace': ['parents', 'SYS-001'], 'compile': ['--root', '.']}[tool]
            args = ['--source=yaml-0.4', '--attribute-schema', 'schema.yaml'] + mode + ['--', filename]
            commands = [[str(BIN / ('mundanereq-' + tool))], ['java', '-cp', CP, 'mundanereq.cli.' + main + 'Main']]
            outputs = []
            for command in commands:
                result = subprocess.run(command + args, cwd=root, capture_output=True, timeout=30)
                assert result.returncode == 0 and not result.stderr, (tool, filename, result.returncode, result.stderr)
                outputs.append(result.stdout)
                if tool == 'compile':
                    artifact = json.loads(result.stdout)
                    assert artifact['complete'] and len(artifact['requirements']) == 2
                    assert artifact['sources'][0]['path'] == filename
                # The same flags before -- remain standalone operations, not input paths.
                result = subprocess.run(command + ['--source=yaml-0.4', '--attribute-schema', 'schema.yaml', '--help'], cwd=root, capture_output=True, timeout=30)
                assert result.returncode == 2 and not result.stdout
            assert outputs[0] == outputs[1]
            assert (root / filename).read_bytes() == original
print('PASS literal -- paths: four option-like filenames, all requirement commands, JVM/native parity and unchanged inputs')
