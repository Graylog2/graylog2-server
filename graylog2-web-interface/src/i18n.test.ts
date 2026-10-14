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
import i18n, { changeLanguage, registerTranslations } from 'i18n';

describe('i18n', () => {
  afterEach(() => changeLanguage('en'));

  it('uses the bundled English translations without loading anything', () => {
    const loadGerman = jest.fn(() => Promise.resolve({ default: { greeting: 'Hallo' } }));

    registerTranslations('test-english', { greeting: 'Hello' }, { de: loadGerman });

    expect(i18n.t('greeting', { ns: 'test-english' })).toBe('Hello');
    expect(loadGerman).not.toHaveBeenCalled();
  });

  it('loads the translations of a language before switching to it', async () => {
    registerTranslations(
      'test-switch',
      { greeting: 'Hello', farewell: 'Bye' },
      { de: () => Promise.resolve({ default: { greeting: 'Hallo' } }) },
    );

    await changeLanguage('de');

    expect(i18n.t('greeting', { ns: 'test-switch' })).toBe('Hallo');
    expect(i18n.t('farewell', { ns: 'test-switch' })).toBe('Bye');
  });

  it('formats values in lower case on request', () => {
    registerTranslations('test-format', { create: 'Create {{entityName, lowercase}}' }, {});

    expect(i18n.t('create', { ns: 'test-format', entityName: 'Event Definition' })).toBe('Create event definition');
  });

  it('loads translations registered after the language has been changed', async () => {
    await changeLanguage('de');

    await registerTranslations(
      'test-late-plugin',
      { greeting: 'Hello' },
      { de: () => Promise.resolve({ default: { greeting: 'Hallo' } }) },
    );

    expect(i18n.t('greeting', { ns: 'test-late-plugin' })).toBe('Hallo');
  });
});
