#!/usr/bin/env python3
"""Inject bounded faults into the real gate, restoring exact input bytes afterward."""
import json
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'build/ci-evidence'
EVIDENCE.mkdir(parents=True, exist_ok=True)


def reject(name, path, mutate, target, marker):
    original = path.read_bytes()
    started = time.monotonic()
    try:
        path.write_bytes(mutate(original))
        result = subprocess.run(['make', 'verify'], cwd=ROOT, capture_output=True)
        log = result.stdout + result.stderr
        (EVIDENCE / (name + '.log')).write_bytes(log)
        if result.returncode == 0 or ('*** [' not in log.decode(errors='replace')) or (target + ']') not in log.decode(errors='replace') or marker not in log.decode(errors='replace'):
            raise RuntimeError(f'{name}: expected attributable {target} failure, got {result.returncode}; see log')
        print(f'PASS {name}: make verify exit={result.returncode}; failing target={target}; elapsed={time.monotonic()-started:.2f}s', flush=True)
    finally:
        path.write_bytes(original)
        if path.read_bytes() != original:
            raise RuntimeError(f'{name}: input restoration failed')
    print(f'PASS {name}: restored exact input bytes', flush=True)


reject('invalid-example', ROOT / 'conformance/0.3/valid/requirements.mreq.yaml',
       lambda original: original.replace(b'      - \"TOP\"\n', b'      - \"ABSENT\"\n', 1), 'test', 'dangling-reference')
reject('omitted-schema-target', ROOT / 'specification/schema/requirements-yaml-0.3.json',
       lambda original: (json.dumps(json.loads(original) | {'type': 'number'}) + '\n').encode(), 'yaml-schema-verify', "not of type 'number'")
print('PASS both injected failures reached the authoritative gate and inputs were restored')
