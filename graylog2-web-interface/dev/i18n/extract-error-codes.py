#!/usr/bin/env python3
"""
Collects the error codes of `Coded{BadRequest,NotFound,Forbidden}Exception("code", "English message"[, Map.of("param", value, ...)])` and
`ValidationResult.addError(field, "code", "English message", Map.of(...))`
from the Java sources and writes/checks the English catalog `src/locales/en/server_errors.json` (code -> English
message with {{param}} placeholders).

The non-literal parts of a message (`"a " + x + " b"` or `f("a %s b", x)`) are matched in order with the keys of `Map.of`.
Sites where that does not work are reported as PROBLEM and need a manual catalog entry.

Usage (from graylog2-web-interface):
  python3 dev/i18n/extract-error-codes.py core [--java-root DIR] --write|--check
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.dirname(os.path.dirname(HERE))
CORE = os.path.dirname(WEB)
ENT = os.path.join(os.path.dirname(CORE), 'graylog-plugin-enterprise')
TARGETS = {'core': (CORE, os.path.join(WEB, 'src/locales/en/server_errors.json')),
           'enterprise': (ENT, os.path.join(ENT, 'enterprise/src/web/locales/en/enterprise_server_errors.json'))}
SITE = re.compile(r'new\s+Coded(?:BadRequest|NotFound|Forbidden)Exception\s*\(|\.addError\s*\(')
STR = r'"(?:[^"\\]|\\.)*"'


def split_args(src, i):
    args, depth, quote, cur = [], 1, False, ''
    while i < len(src) and depth:
        c = src[i]
        if quote:
            cur += c
            if c == '\\':
                cur += src[i + 1]; i += 1
            elif c == '"':
                quote = False
        elif c == '"':
            quote = True; cur += c
        elif c in '([{':
            depth += 1; cur += c
        elif c in ')]}':
            depth -= 1
            if depth: cur += c
        elif c == ',' and depth == 1:
            args.append(cur.strip()); cur = ''
        else:
            cur += c
        i += 1
    args.append(cur.strip())
    return args


def unq(s):
    return json.loads(s) if s.startswith('"') else s


def message_template(expr, keys):
    """English text with {{key}} for every non-literal part, or None."""
    m = re.match(r'^(?:StringUtils\.)?f\(\s*(' + STR + r')\s*,(.*)\)$', expr, re.S)
    if m:
        fmt = unq(m.group(1))
        it = iter(keys)
        try:
            return re.sub(r'%[sd]', lambda _: '{{' + next(it) + '}}', fmt)
        except StopIteration:
            return None
    parts, depth, quote, cur = [], 0, False, ''
    for c in expr:
        if c == '"' and not cur.endswith('\\'):
            quote = not quote
        if not quote:
            if c in '([': depth += 1
            if c in ')]': depth -= 1
            if c == '+' and depth == 0:
                parts.append(cur.strip()); cur = ''; continue
        cur += c
    parts.append(cur.strip())
    it = iter(keys)
    out = ''
    for p in parts:
        if re.fullmatch(STR, p):
            out += unq(p)
        else:
            try: out += '{{' + next(it) + '}}'
            except StopIteration: return None
    return out


def collect(root):
    found, problems = {}, []
    for d, dirs, files in os.walk(root):
        dirs[:] = [x for x in dirs if x not in ('node_modules', 'target', '.git', 'test', 'graylog2-web-interface')]
        for f in files:
            if not f.endswith('.java') or f.startswith('Coded'):
                continue
            p = os.path.join(d, f)
            src = open(p, encoding='utf-8', errors='ignore').read()
            for m in SITE.finditer(src):
                args = split_args(src, m.end())
                if m.group(0).startswith('.addError'):
                    if len(args) != 4:
                        continue  # plain addError(field, message) without code
                    args = args[1:]  # (code, message, params)
                line = src.count('\n', 0, m.start()) + 1
                where = f'{os.path.relpath(p, root)}:{line}'
                if len(args) < 2 or not re.fullmatch(STR, args[0]):
                    problems.append(f'{where}: code is not a literal'); continue
                code = unq(args[0])
                keys = re.findall(r'"(\w+)"\s*,', args[2]) if len(args) > 2 and args[2].startswith(('Map.of', 'CodedException.paramsOf')) else []
                keys = keys[::1]
                text = message_template(args[1], keys)
                if text is None or (len(args) > 2 and keys and len(re.findall(r'\{\{', text)) != len(keys)):
                    problems.append(f'{where}: {code}: placeholders do not match Map.of keys'); continue
                if code in found and found[code] != text:
                    problems.append(f'{where}: {code}: different text than another site'); continue
                found[code] = text
    return found, problems


def main():
    if len(sys.argv) < 3 or sys.argv[1] not in TARGETS or sys.argv[-1] not in ('--write', '--check'):
        print(__doc__); return 2
    root, path = TARGETS[sys.argv[1]]
    if '--java-root' in sys.argv:
        root = sys.argv[sys.argv.index('--java-root') + 1]
    found, problems = collect(root)
    for p in problems: print('PROBLEM', p)
    if sys.argv[-1] == '--write':
        existing = json.load(open(path)) if os.path.exists(path) else {}
        merged = {k: found.get(k, existing.get(k)) for k in sorted(set(found) | set(existing))}
        with open(path, 'w', encoding='utf-8') as fh:
            json.dump(merged, fh, ensure_ascii=False, indent=2); fh.write('\n')
        print(f'{len(found)} codes found, {len(merged)} in {os.path.basename(path)}')
        return 1 if problems else 0
    current = json.load(open(path)) if os.path.exists(path) else {}
    missing = sorted(set(found) - set(current))
    wrong = sorted(k for k in found if k in current and current[k] != found[k])
    print(f'{len(found)} codes in Java, {len(current)} in catalog; missing {len(missing)}, different {len(wrong)}')
    for k in missing: print('  MISSING', k)
    for k in wrong: print('  DIFFERENT', k)
    return 1 if (missing or wrong or problems) else 0


sys.exit(main())
