#!/usr/bin/env python3
"""
Collects the English texts which the server sends to the UI (labels/descriptions of configuration fields and titles
of entity attributes (table columns)) from the Java sources, and writes them as translation catalogs.

The catalogs use the English text itself as key (and as English value), see `src/util/serverText.ts`:
  core:        src/locales/en/server_fields.json, src/locales/en/entity_attributes.json
  enterprise:  enterprise/src/web/locales/en/enterprise_server_fields.json, ..._entity_attributes.json

Usage (from graylog2-web-interface):
  python3 dev/i18n/extract-server-fields.py core --write        # regenerate the English catalogs
  python3 dev/i18n/extract-server-fields.py core --check        # exit 1 when a text found in Java is missing in the catalog
  python3 dev/i18n/extract-server-fields.py enterprise --write|--check

Only plain string literals (and `"a" + "b"` concatenations) are found. Texts built from constants or formatted at
runtime stay English; that is fine, they simply fall back to what the server sends.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.dirname(os.path.dirname(HERE))  # graylog2-web-interface
CORE_REPO = os.path.dirname(WEB)
ENTERPRISE_REPO = os.path.join(os.path.dirname(CORE_REPO), 'graylog-plugin-enterprise')

TARGETS = {
    'core': {
        'repo': CORE_REPO,
        'fields': os.path.join(WEB, 'src/locales/en/server_fields.json'),
        'attributes': os.path.join(WEB, 'src/locales/en/entity_attributes.json'),
    },
    'enterprise': {
        'repo': ENTERPRISE_REPO,
        'fields': os.path.join(ENTERPRISE_REPO, 'enterprise/src/web/locales/en/enterprise_server_fields.json'),
        'attributes': os.path.join(ENTERPRISE_REPO, 'enterprise/src/web/locales/en/enterprise_entity_attributes.json'),
    },
}

# constructor argument positions (name, humanName, ...) of the configuration fields: (human name, description)
FIELD_ARGS = {
    'TextField': (1, 3),
    'BooleanField': (1, 3),
    'NumberField': (1, 3),
    'MapField': (1, 3),
    'DropdownField': (1, 4),
    'ListField': (1, 4),
}
FIELD_RE = re.compile(r'new\s+(' + '|'.join(FIELD_ARGS) + r')\s*\(')
STRING = r'"(?:[^"\\]|\\.)*"'
LITERAL_RE = re.compile(r'^\s*' + STRING + r'(?:\s*\+\s*' + STRING + r')*\s*$', re.S)
ATTRIBUTE_TITLE_RE = re.compile(r'\.title\(\s*(' + STRING + r'(?:\s*\+\s*' + STRING + r')*)\s*\)', re.S)
JAVA_ESCAPES = {'n': '\n', 't': '\t', 'r': '\r', '"': '"', "'": "'", '\\': '\\'}


def unescape(raw):
    out = []
    i = 0
    while i < len(raw):
        c = raw[i]
        if c == '\\' and i + 1 < len(raw):
            n = raw[i + 1]
            if n == 'u' and i + 5 < len(raw) + 1:
                out.append(chr(int(raw[i + 2:i + 6], 16)))
                i += 6
                continue
            out.append(JAVA_ESCAPES.get(n, n))
            i += 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def literal(expr):
    """Value of a Java string literal expression, or None."""
    if not LITERAL_RE.match(expr):
        return None
    return ''.join(unescape(part[1:-1]) for part in re.findall(STRING, expr))


def split_args(source, start):
    """Arguments of the call whose '(' ends at index `start`."""
    args, depth, quote, current = [], 1, None, ''
    i = start
    while i < len(source) and depth:
        c = source[i]
        if quote:
            current += c
            if c == '\\':
                current += source[i + 1]
                i += 1
            elif c == quote:
                quote = None
        elif c == '"':
            quote = c
            current += c
        elif c in '([{':
            depth += 1
            current += c
        elif c in ')]}':
            depth -= 1
            if depth:
                current += c
        elif c == ',' and depth == 1:
            args.append(current)
            current = ''
        else:
            current += c
        i += 1
    args.append(current)
    return args


def java_files(repo):
    for directory, dirs, files in os.walk(repo):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'target', '.git', 'test', 'graylog2-web-interface')]
        if '/src/main/java' not in directory.replace(os.sep, '/') + '/':
            continue
        for f in files:
            if f.endswith('.java'):
                yield os.path.join(directory, f)


def collect(repo):
    fields, attributes = set(), set()
    for path in java_files(repo):
        try:
            source = open(path, encoding='utf-8').read()
        except Exception:
            continue
        for m in FIELD_RE.finditer(source):
            args = split_args(source, m.end())
            human_index, description_index = FIELD_ARGS[m.group(1)]
            for index in (human_index, description_index):
                if index < len(args):
                    text = literal(args[index])
                    if text and re.search(r'[A-Za-z]{2}', text):
                        fields.add(text)
        if 'EntityAttribute' in source:
            for m in ATTRIBUTE_TITLE_RE.finditer(source):
                text = literal(m.group(1))
                if text and re.search(r'[A-Za-z]', text):
                    attributes.add(text)
    return fields, attributes


def write(path, texts):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump({t: t for t in sorted(texts)}, f, ensure_ascii=False, indent=2)
        f.write('\n')


def main():
    if len(sys.argv) != 3 or sys.argv[1] not in TARGETS or sys.argv[2] not in ('--write', '--check'):
        print(__doc__)
        return 2
    target = TARGETS[sys.argv[1]]
    fields, attributes = collect(target['repo'])
    if sys.argv[2] == '--write':
        write(target['fields'], fields)
        write(target['attributes'], attributes)
        print(f'{len(fields)} field texts, {len(attributes)} attribute titles written')
        return 0
    status = 0
    for name, found in (('fields', fields), ('attributes', attributes)):
        path = target[name]
        current = set(json.load(open(path))) if os.path.exists(path) else set()
        missing, orphans = sorted(found - current), sorted(current - found)
        print(f'{name}: {len(found)} found in Java, {len(current)} in {os.path.basename(path)}; missing {len(missing)}, orphans {len(orphans)}')
        for t in missing[:10]:
            print('  MISSING:', t[:100])
        for t in orphans[:5]:
            print('  orphan:', t[:100])
        if missing:
            status = 1
    return status


if __name__ == '__main__':
    sys.exit(main())
