#!/usr/bin/env python3
"""Independent structural schema check; source presentation/domain rules are separate."""
from pathlib import Path
import json
from jsonschema import Draft202012Validator
from ruamel.yaml import YAML

root = Path(__file__).resolve().parents[1]
yaml = YAML(typ='safe', pure=True)
yaml.version = (1, 2)
schema = json.loads((root / 'specification/schema/requirements-yaml-0.3.json').read_text())
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema)
valid_count = 0
for line in (root / 'conformance/0.3/corpora.txt').read_text().splitlines():
    folder = line
    for path in sorted((root / folder).glob('*.mreq.yaml')):
        data = yaml.load(path.read_text())
        errors = list(validator.iter_errors(data))
        assert not errors, (path, [e.message for e in errors])
        valid_count += 1
for path in (root / 'conformance/0.3/authoring').glob('*.mreq.yaml'):
    assert validator.is_valid(yaml.load(path.read_text())), path
    valid_count += 1
negative_count = 0
for line in (root / 'conformance/0.3/invalid-cases.tsv').read_text().splitlines():
    name, _, _, _, expected = line.split('\t')
    if expected == 'none':
        continue  # Duplicate keys must be checked before schema object construction.
    path = root / 'conformance/0.3/invalid' / name
    actual = validator.is_valid(yaml.load(path.read_text()))
    assert actual == (expected == 'true'), (name, actual, expected)
    negative_count += 1
print(f'PASS independent Draft 2020-12 schema: {valid_count} valid files, {negative_count} structural/domain distinction cases')
