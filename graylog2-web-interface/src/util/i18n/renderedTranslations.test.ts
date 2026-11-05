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
import i18n from 'i18n';

import { clearRenderedTranslations, findTranslations, recordTranslation } from './renderedTranslations';

const source = (key: string, namespace = 'test') => ({ namespace, key, usedLanguage: 'de' });

describe('renderedTranslations', () => {
  beforeEach(() => clearRenderedTranslations());

  it('finds texts rendered exactly like this', () => {
    recordTranslation('Suche  starten ', source('start'));

    expect(findTranslations('Suche starten')).toEqual([{ ...source('start'), text: 'Suche starten', exact: true }]);
  });

  it('returns all keys rendering the same text', () => {
    recordTranslation('Speichern', source('save'));
    recordTranslation('Speichern', source('actions.save', 'common'));

    expect(findTranslations('Speichern').map(({ key }) => key)).toEqual(['save', 'actions.save']);
  });

  it('ignores the tags of `<Trans>` components', () => {
    recordTranslation('Mehr in der <1>Dokumentation</1>.', source('docs'));

    expect(findTranslations('Mehr in der Dokumentation.')).toEqual([
      expect.objectContaining({ key: 'docs', exact: true }),
    ]);
  });

  it('finds texts contained in the clicked text, closest first', () => {
    recordTranslation('Nachrichten', source('messages'));
    recordTranslation('Nachrichten der letzten 5 Minuten', source('messages_in_range'));

    expect(findTranslations('Nachrichten der letzten 5 Minuten (42)').map(({ key }) => key)).toEqual([
      'messages_in_range',
      'messages',
    ]);
  });

  it('ignores texts which only share a part of a word or a small part of a translation', () => {
    recordTranslation('OpenSearch cluster', source('cluster'));
    recordTranslation('Notes', source('notes'));
    recordTranslation('Collapse Cluster Health', source('collapse'));

    expect(findTranslations('Open')).toEqual([]);
    expect(findTranslations('No')).toEqual([]);
    expect(findTranslations('Cluster Health')).toEqual([expect.objectContaining({ key: 'collapse', exact: false })]);
    expect(findTranslations('Health')).toEqual([]);
  });

  it('returns nothing for texts which were not translated', () => {
    recordTranslation('Suche', source('search'));

    expect(findTranslations('Hardcoded text')).toEqual([]);
    expect(findTranslations('  ')).toEqual([]);
  });

  it('is filled by translating texts', () => {
    i18n.addResourceBundle('en', 'rendered_translations_test', { greeting: 'Hello {{name}}' });

    i18n.t('greeting', { ns: 'rendered_translations_test', name: 'Ada' });

    expect(findTranslations('Hello Ada')).toEqual([
      expect.objectContaining({ namespace: 'rendered_translations_test', key: 'greeting', exact: true }),
    ]);
  });
});
