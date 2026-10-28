#!/usr/bin/env python3
"""
Lists translation keys used in source files that don't exist in the English locale files.

Usage (from graylog2-web-interface):
  python3 dev/i18n/find-missing-keys.py                 # all changed files (git status)
  python3 dev/i18n/find-missing-keys.py src/components/inputs src/pages/InputsPage.tsx

Heuristic, not a parser: it understands `useTranslation('ns')`, `t('key')`, `t('ns:key')`, `t('key', { ns: 'ns' })`,
`i18n.t('key', { ns: 'ns' })` and `i18nKey="key"`. Keys built dynamically (e.g. from key maps or template
literals) are not checked; the Jest missing-key guard catches those for rendered components.
"""
import json
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LOCALES = os.path.join(ROOT, 'src', 'locales', 'en')
PLURAL_SUFFIXES = ('_zero', '_one', '_two', '_few', '_many', '_other')

USE_TRANSLATION = re.compile(r"useTranslation\(\s*['\"]([\w-]+)['\"](?P<rest>[^)]*)")
KEY_PREFIX = re.compile(r"keyPrefix:\s*['\"]([\w.]+)['\"]")
T_CALL = re.compile(r"(?<![\w.])t\(\s*['\"]([^'\"]+)['\"](?P<rest>[^)]*)")
I18N_T_CALL = re.compile(r"i18n\.t\(\s*['\"]([^'\"]+)['\"](?P<rest>[^)]*)")
NS_OPTION = re.compile(r"ns:\s*['\"]([\w-]+)['\"]")
I18N_KEY = re.compile(r"i18nKey=['\"]([^'\"]+)['\"]")


def load_keys():
    keys = {}
    for file in os.listdir(LOCALES):
        if not file.endswith('.json'):
            continue
        namespace = file[:-len('.json')]
        flat = set()

        def walk(node, prefix=''):
            for key, value in node.items():
                if isinstance(value, dict):
                    walk(value, f'{prefix}{key}.')
                else:
                    flat.add(f'{prefix}{key}')

        with open(os.path.join(LOCALES, file), encoding='utf-8') as f:
            walk(json.load(f))
        keys[namespace] = flat
    return keys


def exists(keys, namespace, key):
    available = keys.get(namespace, set())
    return key in available or any(f'{key}{suffix}' in available for suffix in PLURAL_SUFFIXES)


def split(key, default_namespace):
    if ':' in key:
        namespace, key = key.split(':', 1)
        return namespace, key
    return default_namespace, key


def files_to_check(args):
    if args:
        paths = []
        for arg in args:
            if os.path.isdir(arg):
                for directory, _, files in os.walk(arg):
                    paths += [os.path.join(directory, f) for f in files]
            else:
                paths.append(arg)
    else:
        status = subprocess.run(['git', 'status', '--porcelain', '-uall', '--', 'src'], cwd=ROOT,
                                capture_output=True, text=True, check=True).stdout
        paths = [os.path.join(ROOT, os.path.relpath(line[3:], 'graylog2-web-interface')) if line[3:].startswith('graylog2-web-interface/')
                 else os.path.join(ROOT, line[3:]) for line in status.splitlines()]
    return [p for p in paths if p.endswith(('.ts', '.tsx')) and '.test.' not in p and os.path.isfile(p)]


def main():
    keys = load_keys()
    missing_total = 0
    for path in files_to_check(sys.argv[1:]):
        with open(path, encoding='utf-8') as f:
            source = f.read()
        use_translation = USE_TRANSLATION.search(source)
        default_namespace = use_translation.group(1) if use_translation else None
        prefix_match = KEY_PREFIX.search(use_translation.group('rest')) if use_translation else None
        key_prefix = f'{prefix_match.group(1)}.' if prefix_match else ''
        missing = []
        for match in T_CALL.finditer(source):
            ns_match = NS_OPTION.search(match.group('rest'))
            namespace, key = split(match.group(1), ns_match.group(1) if ns_match else default_namespace)
            if namespace == default_namespace and not ns_match:
                key = f'{key_prefix}{key}'
            if namespace and not exists(keys, namespace, key):
                missing.append(f'{namespace}:{key}')
        for match in I18N_KEY.finditer(source):
            namespace, key = split(match.group(1), default_namespace)
            if namespace == default_namespace:
                key = f'{key_prefix}{key}'
            if namespace and not exists(keys, namespace, key):
                missing.append(f'{namespace}:{key}')
        for match in I18N_T_CALL.finditer(source):
            ns_match = NS_OPTION.search(match.group('rest'))
            namespace, key = split(match.group(1), ns_match.group(1) if ns_match else None)
            if namespace and not exists(keys, namespace, key):
                missing.append(f'{namespace}:{key}')
        if missing:
            missing_total += len(set(missing))
            print(os.path.relpath(path, ROOT))
            for key in sorted(set(missing)):
                print(f'  {key}')
    print(f'{missing_total} missing keys')
    return 1 if missing_total else 0


if __name__ == '__main__':
    sys.exit(main())
