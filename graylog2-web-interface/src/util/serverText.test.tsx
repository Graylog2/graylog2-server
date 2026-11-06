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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';

import i18n from 'i18n';

import useServerText, { translateServerText } from './serverText';

describe('serverText', () => {
  beforeAll(() => {
    // The English texts contain dots and colons, which must not be read as key or namespace separators.
    i18n.addResourceBundle('en', 'server_fields', {
      'Bind address': 'Bind address',
      'Port: e.g. 514.': 'Port: e.g. 514.',
      'Archived documents in indices: {{indices}}': 'Archived documents in indices: {{indices}}',
      'Restored {{documents}} documents from index: {{index}}': 'Restored {{documents}} documents from index: {{index}}',
    });
    i18n.addResourceBundle('de', 'server_fields', {
      'Bind address': 'Bind-Adresse',
      'Port: e.g. 514.': 'Port: z. B. 514.',
      'Archived documents in indices: {{indices}}': 'Dokumente in Indizes archiviert: {{indices}}',
      'Restored {{documents}} documents from index: {{index}}': '{{documents}} Dokumente aus Index {{index}} wiederhergestellt',
    });
  });

  afterEach(async () => {
    await act(async () => {
      await i18n.changeLanguage('en');
    });
  });

  it('keeps the text of the server for English', () => {
    expect(translateServerText('Bind address')).toBe('Bind address');
  });

  it('translates texts which contain dots and colons', async () => {
    await i18n.changeLanguage('de');

    expect(translateServerText('Port: e.g. 514.')).toBe('Port: z. B. 514.');
  });

  it('keeps the text of the server when there is no translation', async () => {
    await i18n.changeLanguage('de');

    expect(translateServerText('Some new text of the server')).toBe('Some new text of the server');
    expect(translateServerText(undefined)).toBeUndefined();
  });

  it('translates texts which contain values', async () => {
    await i18n.changeLanguage('de');

    expect(translateServerText('Archived documents in indices: graylog_1, graylog_2')).toBe(
      'Dokumente in Indizes archiviert: graylog_1, graylog_2',
    );
    expect(translateServerText('Restored 42 documents from index: graylog_3')).toBe(
      '42 Dokumente aus Index graylog_3 wiederhergestellt',
    );
    expect(translateServerText('Archived documents in indices:')).toBe('Archived documents in indices:');
  });

  it('follows language changes in components', async () => {
    const { result } = renderHook(() => useServerText());

    expect(result.current('Bind address')).toBe('Bind address');

    await act(async () => {
      await i18n.changeLanguage('de');
    });

    expect(result.current('Bind address')).toBe('Bind-Adresse');
  });
});
