"""Generate Java constants and package metadata from the current declarations."""
import json
import re
import sys
from pathlib import Path


def read(path):
    values = {}
    for line in Path(path).read_text().splitlines():
        if not line or line.startswith('#'):
            continue
        match = re.fullmatch(r'([A-Z][A-Z_0-9]*)=([a-zA-Z0-9._+\-]+)', line)
        if not match or match[1] in values:
            raise ValueError('invalid or duplicate version declaration: ' + line)
        values[match[1]] = match[2]
    required = {'ATTRIBUTE_SOURCE', 'EDITOR_VERSION', 'EDITOR_PROTOCOL', 'EDITOR_PROJECT', 'IMPACT_VERSION', 'IMPACT_CONTRACT', 'IMPACT_ARTIFACT', 'FORMAT_ATTRIBUTE_CONTRACT','TRACE_ATTRIBUTE_CONTRACT','REQUIREMENT_ATTRIBUTE_ARTIFACT','COMPILE_ATTRIBUTE_CONTRACT','LINK_ATTRIBUTE_ARTIFACT','LINK_ATTRIBUTE_CONTRACT','VERIFICATION_ATTRIBUTE_ARTIFACT','VERIFY_ATTRIBUTE_CONTRACT','SOURCE_ATTRIBUTES','ATTRIBUTE_SCHEMA','VALIDATE_ATTRIBUTE_CONTRACT','WORK_SOURCE', 'WORK_SET', 'WORK_ARTIFACT', 'WORK_ANALYSIS', 'WORK_VERSION', 'WORK_CONTRACT', 'SARIF_VERSION', 'SOURCE_YAML', 'SUITE_VERSION', 'REQUIREMENT_ARTIFACT', 'IMPORT_FORMAT', 'LINK_ARTIFACT', 'PLAN_ARTIFACT', 'PLAN_SOURCE', 'VERIFICATION_ARTIFACT'} | {tool+suffix for tool in ['VALIDATE','FORMAT','TRACE','COMPILE','LINK','PLAN','VERIFY'] for suffix in ['_VERSION','_CONTRACT']}
    if not required <= values.keys():
        raise ValueError('missing declarations: '+str(sorted(required-values.keys())))
    return values


def generate(values, output):
    output = Path(output)
    java = output/'mundanereq/Versions.java'
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text('package mundanereq;\n\n/** Generated from versions.properties; do not edit. */\npublic final class Versions {\n    private Versions() {}\n'
                    + ''.join('    public static final String '+key+' = '+json.dumps(value)+';\n' for key,value in sorted(values.items()))+'}\n')
    for domain in ['architecture','configuration','safety','procedure','software','equipment']:
        schema=Path(__file__).resolve().parents[1]/'specification/schema'/(domain+'-yaml-0.1.json')
        target=output/'engineering'/domain/(domain.title()+'Schema.java');target.parent.mkdir(parents=True,exist_ok=True)
        encoded=json.dumps(json.dumps(json.loads(schema.read_text()),separators=(',',':')))
        target.write_text('package engineering.'+domain+'; public final class '+domain.title()+'Schema { private '+domain.title()+'Schema() {} public static final String JSON = '+encoded+'; }\n')
    import hashlib
    repository=Path(__file__).resolve().parents[1]
    for name,contract in [('Run','run'),('Manual','manual-observation-yaml'),('Assessment','assessment-yaml')]:
        schema=repository/'specification/schema'/(contract+'-0.1.json')
        target=output/'engineering/evidence'/(name+'Schema.java');target.parent.mkdir(parents=True,exist_ok=True)
        encoded=json.dumps(json.dumps(json.loads(schema.read_text()),separators=(',',':')))
        target.write_text('package engineering.evidence; public final class '+name+'Schema { private '+name+'Schema() {} public static final String JSON = '+encoded+'; }\n')
    inputs=sorted([*repository.glob('src/main/java/**/*.java'),*repository.glob('specification/schema/*.json'),repository/'versions.properties'])
    fingerprint=hashlib.sha256(b''.join(str(p.relative_to(repository)).encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest() for p in inputs)).hexdigest()
    (output/'engineering/evidence/EvidenceBuild.java').write_text('package engineering.evidence; public final class EvidenceBuild { private EvidenceBuild() {} public static final String SHA256 = "'+fingerprint+'"; }\n')
    (output/'versions.json').write_text(json.dumps(values, sort_keys=True, indent=2)+'\n')


if __name__ == '__main__':
    generate(read(sys.argv[1]), sys.argv[2])
