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
    required = {'SOURCE_ATTRIBUTES','ATTRIBUTE_SCHEMA','VALIDATE_ATTRIBUTE_CONTRACT','WORK_SOURCE', 'WORK_SET', 'WORK_ARTIFACT', 'WORK_ANALYSIS', 'WORK_VERSION', 'WORK_CONTRACT', 'SARIF_VERSION', 'SOURCE_CUSTOM', 'SOURCE_YAML', 'SUITE_VERSION', 'REQUIREMENT_ARTIFACT', 'IMPORT_FORMAT', 'LINK_ARTIFACT', 'PLAN_ARTIFACT', 'PLAN_SOURCE', 'VERIFICATION_ARTIFACT'} | {tool+suffix for tool in ['VALIDATE','FORMAT','TRACE','MIGRATE','COMPILE','LINK','PLAN','VERIFY'] for suffix in ['_VERSION','_CONTRACT']}
    if not required <= values.keys():
        raise ValueError('missing declarations: '+str(sorted(required-values.keys())))
    return values


def generate(values, output):
    output = Path(output)
    java = output/'mundanereq/Versions.java'
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text('package mundanereq;\n\n/** Generated from versions.properties; do not edit. */\npublic final class Versions {\n    private Versions() {}\n'
                    + ''.join('    public static final String '+key+' = '+json.dumps(value)+';\n' for key,value in sorted(values.items()))+'}\n')
    (output/'versions.json').write_text(json.dumps(values, sort_keys=True, indent=2)+'\n')


if __name__ == '__main__':
    generate(read(sys.argv[1]), sys.argv[2])
