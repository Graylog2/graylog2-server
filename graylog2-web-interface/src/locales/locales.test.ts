/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as fs from 'fs';
import * as path from 'path';

const LOCALES_PATH = __dirname;
const DEFAULT_LANGUAGE = 'en';
const PLURAL_SUFFIX = /_(zero|one|two|few|many|other)$/;

type Translations = { [key: string]: string | Translations };

const flatten = (translations: Translations, prefix = ''): Array<[string, string]> =>
  Object.entries(translations).flatMap(([key, value]) =>
    typeof value === 'string' ? [[`${prefix}${key}`, value]] : flatten(value, `${prefix}${key}.`),
  );

const readNamespace = (language: string, file: string) =>
  JSON.parse(fs.readFileSync(path.join(LOCALES_PATH, language, file), 'utf8')) as Translations;

const placeholders = (text: string) =>
  // Formats like `{{name, lowercase}}` may differ between languages, only the placeholder names have to match.
  [...text.matchAll(/{{\s*([\w.]+)\s*(?:,[^}]*)?}}/g)].map(([, name]) => name).filter((name) => name !== 'count');

// Languages differ in their plural forms and may need extra contexts (e.g. a grammatical case), so translations are
// compared by their key without plural suffix. Keys with an additional context fall back to their key without it.
const baseKey = (key: string) => key.replace(PLURAL_SUFFIX, '');

const englishPlaceholdersByKey = (file: string) => {
  const byKey = new Map<string, Set<string>>();

  flatten(readNamespace(DEFAULT_LANGUAGE, file)).forEach(([key, text]) => {
    byKey.set(baseKey(key), new Set([...(byKey.get(baseKey(key)) ?? []), ...placeholders(text)]));
  });

  return byKey;
};

const languages = fs
  .readdirSync(LOCALES_PATH, { withFileTypes: true })
  .filter((entry) => entry.isDirectory() && entry.name !== DEFAULT_LANGUAGE)
  .map(({ name }) => name);
const namespaceFiles = fs
  .readdirSync(path.join(LOCALES_PATH, DEFAULT_LANGUAGE))
  .filter((file) => file.endsWith('.json'));

describe.each(languages)('translations for "%s"', (language) => {
  it('exist for all namespaces', () => {
    expect(fs.readdirSync(path.join(LOCALES_PATH, language)).sort()).toEqual(namespaceFiles.sort());
  });

  describe.each(namespaceFiles)('in %s', (file) => {
    const english = englishPlaceholdersByKey(file);
    const translations = flatten(readNamespace(language, file));
    const englishKeyFor = (key: string) =>
      [baseKey(key), baseKey(key).replace(/_[a-z]+$/, '')].find((candidate) => english.has(candidate));

    it('contain all keys', () => {
      const translatedKeys = new Set(translations.map(([key]) => englishKeyFor(key)));

      expect([...english.keys()].filter((key) => !translatedKeys.has(key))).toEqual([]);
    });

    it('contain no unknown keys', () => {
      expect(translations.filter(([key]) => !englishKeyFor(key)).map(([key]) => key)).toEqual([]);
    });

    it('use the same placeholders', () => {
      const mismatches = translations
        .filter(([key]) => englishKeyFor(key))
        .filter(([key, text]) => {
          const expected = english.get(englishKeyFor(key));
          const actual = new Set(placeholders(text));

          return expected.size !== actual.size || [...expected].some((name) => !actual.has(name));
        })
        .map(([key]) => key);

      expect(mismatches).toEqual([]);
    });
  });
});
