"""Fixture YAML I/O using the existing Ruby build prerequisite, never a production parser."""
import json
import subprocess


def loads(text):
    result = subprocess.run(['ruby', '-rjson', '-ryaml', '-e',
                             'STDOUT.write(JSON.generate(YAML.safe_load(STDIN.read, aliases: false)))'],
                            input=text, text=True, capture_output=True, check=True)
    return json.loads(result.stdout)


def dumps(value):
    # Emit the deliberately small fixture model with quoted string values. This
    # also meets requirements' stricter scalar-style rules without coercion.
    def emit(item, indent):
        pad = ' ' * indent
        if isinstance(item, dict) and item:
            return ''.join(pad + json.dumps(key) + ':' + nested(val, indent) for key, val in item.items())
        if isinstance(item, list) and item:
            return ''.join(pad + '-' + nested(val, indent) for val in item)
        return pad + json.dumps(item, ensure_ascii=False) + '\n'
    def nested(item, indent):
        if isinstance(item, (dict, list)) and item:
            return '\n' + emit(item, indent + 2)
        return ' ' + json.dumps(item, ensure_ascii=False) + '\n'
    return emit(value, 0)
