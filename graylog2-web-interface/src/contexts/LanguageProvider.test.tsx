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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { useTranslation } from 'react-i18next';

import { changeLanguage, registerTranslations } from 'i18n';
import Store from 'logic/local-storage/Store';
import { lastLanguage } from 'util/applyLanguage';

import LanguageProvider from './LanguageProvider';

const Greeting = () => {
  const { t } = useTranslation('test-provider');

  return <span>{t('greeting')}</span>;
};

const loadGerman = () => Promise.resolve({ default: { greeting: 'Hallo' } });

describe('LanguageProvider', () => {
  beforeAll(() => {
    registerTranslations('test-provider', { greeting: 'Hello' }, { de: loadGerman });
  });

  afterEach(async () => {
    Store.delete('language');
    await changeLanguage('en');
  });

  it('renders children only in the requested language', async () => {
    render(
      <LanguageProvider language="de">
        <Greeting />
      </LanguageProvider>,
    );

    await screen.findByText('Hallo');

    expect(screen.queryByText('Hello')).not.toBeInTheDocument();
  });

  it('switches language of rendered children', async () => {
    const { rerender } = render(
      <LanguageProvider language="en">
        <Greeting />
      </LanguageProvider>,
    );

    await screen.findByText('Hello');

    rerender(
      <LanguageProvider language="de">
        <Greeting />
      </LanguageProvider>,
    );

    await screen.findByText('Hallo');
  });

  it('remembers the language for pages shown before login', async () => {
    render(
      <LanguageProvider language="de">
        <Greeting />
      </LanguageProvider>,
    );

    await screen.findByText('Hallo');

    expect(lastLanguage()).toBe('de');
  });
});
