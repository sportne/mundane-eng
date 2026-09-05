"""Independent schemas validate decoded structure; source/graph rules belong to tools."""
import json,copy
from pathlib import Path
from jsonschema import Draft202012Validator
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];yaml=YAML(typ='safe',pure=True);yaml.version=(1,2)
for schema_name,fixture in [('attribute-declaration-0.1.json','requirement-attributes.json'),('requirements-yaml-0.4.json','system.mreq.yaml')]:
    schema=json.loads((ROOT/'specification/schema'/schema_name).read_text());Draft202012Validator.check_schema(schema);v=Draft202012Validator(schema);data=yaml.load((ROOT/'examples/attributes'/fixture).read_text());v.validate(data)
    for change in [{'format':'future'},{'unknown':True}]:assert not v.is_valid(data|change)
print('PASS independent attribute declaration and YAML 0.4 structural schemas')
