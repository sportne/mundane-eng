"""Minimal contract-only consumer: parser classes and source syntax are unavailable."""
import json
import sys
from pathlib import Path


def unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('duplicate JSON key')
        result[key] = value
    return result


def consume(text):
    artifact = json.loads(text, object_pairs_hook=unique)
    if artifact['artifactKind'] != 'requirements' or artifact['format'] != 'mundanereq-requirements-0.1':
        raise ValueError('unsupported artifact format or kind')
    if artifact['sourceContract'] not in [ 'mundanereq-yaml-0.3']:
        raise ValueError('unsupported source contract')
    if artifact['complete'] is not True or artifact['diagnostics']:
        raise ValueError('incomplete artifact; analysis prohibited')
    ids = set()
    for record in artifact['requirements']:
        values = record['values']
        if values['id'] in ids:
            raise ValueError('duplicate requirement ID')
        ids.add(values['id'])
        for name in ['statement', 'rationale']:
            for block in values[name] or []:
                if block['kind'] not in ['prose','math']:
                    raise ValueError('unknown semantic block')
                if block['kind'] == 'math' and block['language'] != 'latex':
                    raise ValueError('unknown math language')
        locations = record['locations']
        for target in values['decomposes']:
            if target not in locations['references']:
                raise ValueError('missing authored reference location')
    for record in artifact['requirements']:
        if not set(record['values']['decomposes']) <= ids:
            raise ValueError('unresolved relationship')
    return sorted(ids)


if __name__ == '__main__':
    try:
        print('\n'.join(consume(Path(sys.argv[1]).read_text())))
    except (ValueError, KeyError, TypeError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
