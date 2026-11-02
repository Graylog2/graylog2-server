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
import FetchError from 'logic/errors/FetchError';

import translateApiError from './translateApiError';
import extractErrorMessage from './extractErrorMessage';

describe('translateApiError', () => {
  beforeAll(() => {
    i18n.addResourceBundle('en', 'server_errors', { 'test.title_exists': 'Title "{{title}}" exists' }, true, true);
    i18n.addResourceBundle('de', 'server_errors', { 'test.title_exists': 'Titel "{{title}}" existiert' }, true, true);
  });

  const error = (body: object) => new FetchError('Bad Request', 400, body);

  it('translates by code and fills the params', () => {
    expect(translateApiError(error({ code: 'test.title_exists', message: 'x', params: { title: 'Foo' } }))).toBe(
      'Title "Foo" exists',
    );
  });

  it('returns undefined without code, with unknown code or for other errors', () => {
    expect(translateApiError(error({ message: 'x' }))).toBeUndefined();
    expect(translateApiError(error({ code: 'test.unknown', message: 'x' }))).toBeUndefined();
    expect(translateApiError(new Error('boom'))).toBeUndefined();
    expect(translateApiError(undefined)).toBeUndefined();
  });

  it('follows the language', async () => {
    await i18n.changeLanguage('de');

    expect(translateApiError(error({ code: 'test.title_exists', params: { title: 'Foo' } }))).toBe('Titel "Foo" existiert');

    await i18n.changeLanguage('en');
  });

  it('is used by extractErrorMessage, which keeps the server message without translation', () => {
    expect(extractErrorMessage(error({ code: 'test.title_exists', message: 'x', params: { title: 'Foo' } }))).toBe(
      'Title "Foo" exists',
    );
    expect(extractErrorMessage(error({ code: 'test.unknown', message: 'English from server' }))).toBe(
      'English from server',
    );
  });
});
