#!/usr/bin/env python3
"""
Lists the places in the Java REST layer which throw an error with a literal English message that the web interface could
translate once the exception carries an error code (see CodedBadRequestException etc.).

Usage (from graylog2-web-interface):
  python3 dev/i18n/list-api-errors.py [--repo core|enterprise] [--root DIR] [--files]   # counts per package (default) or every site
"""
import os, re, sys, collections

HERE = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.dirname(os.path.dirname(HERE))
CORE = os.path.dirname(WEB)
REPOS = {'core': CORE, 'enterprise': os.path.join(os.path.dirname(CORE), 'graylog-plugin-enterprise')}
EXC = r'(?:BadRequest|NotFound|Forbidden)Exception'
SITE = re.compile(r'new\s+(' + EXC + r')\s*\(\s*("(?:[^"\\]|\\.)*"(?:\s*\+\s*[^;]*)?)\)\s*;?')


def main():
    repo = REPOS[sys.argv[sys.argv.index('--repo') + 1]] if '--repo' in sys.argv else CORE
    if '--root' in sys.argv:
        repo = sys.argv[sys.argv.index('--root') + 1]
    show = '--files' in sys.argv
    counts = collections.Counter()
    for d, dirs, files in os.walk(repo):
        dirs[:] = [x for x in dirs if x not in ('node_modules', 'target', '.git', 'test', 'graylog2-web-interface')]
        if '/src/main/java' not in d + '/':
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            p = os.path.join(d, f)
            if '/rest/' not in p and 'Resource' not in f:
                continue
            src = open(p, encoding='utf-8', errors='ignore').read()
            for m in SITE.finditer(src):
                line = src.count('\n', 0, m.start()) + 1
                pkg = d.split('/src/main/java/')[1].split('/')
                counts['.'.join(pkg[:5])] += 1
                if show:
                    print(f'{os.path.relpath(p, repo)}:{line}: {m.group(1)} {m.group(2)[:90]}')
    if not show:
        for k, v in counts.most_common():
            print(f'{v:5} {k}')
        print(f'{sum(counts.values()):5} TOTAL')


main()
