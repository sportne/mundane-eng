"""Check (or explicitly regenerate) editor metadata from authoritative declarations."""
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('versions', ROOT / 'scripts/generate-versions.py')
versions = importlib.util.module_from_spec(spec)
spec.loader.exec_module(versions)

def expected(values):
    return {'version': values['EDITOR_VERSION'], 'protocol': values['EDITOR_PROTOCOL'], 'project': values['EDITOR_PROJECT']}

def synchronize(root, values, write=False):
    editor = root / 'editors/vscode'
    metadata = expected(values)
    for name, transform in [
        ('versions.json', lambda old: metadata),
        ('package.json', lambda old: dict(old, version=metadata['version'])),
        ('package-lock.json', lambda old: dict(old, version=metadata['version'], packages=dict(old['packages'], **{'':dict(old['packages'][''],version=metadata['version'])})))]:
        path = editor / name
        old = json.loads(path.read_text()) if path.exists() else {}
        new = transform(old)
        if write:
            path.write_text(json.dumps(new, indent=2) + '\n')
        elif old != new:
            raise ValueError(f'{name} disagrees with versions.properties; run python3 scripts/editor-versions.py --write')
    return metadata

if __name__ == '__main__':
    if sys.argv[1:] not in [[], ['--write']]: raise SystemExit('usage: editor-versions.py [--write]')
    synchronize(ROOT, versions.read(ROOT / 'versions.properties'), bool(sys.argv[1:]))
    print('PASS editor manifest, lockfile and runtime metadata agree with versions.properties')
