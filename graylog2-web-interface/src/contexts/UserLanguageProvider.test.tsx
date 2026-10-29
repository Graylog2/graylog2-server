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
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import Store from 'logic/local-storage/Store';
import UserPreferencesContext, { defaultUserPreferences } from 'contexts/UserPreferencesContext';

import UserLanguageProvider from './UserLanguageProvider';

jest.mock('hooks/useCurrentUser');
jest.mock('./LanguageProvider', () => ({
  __esModule: true,
  default: ({ language }: { language: string }) => `language: ${language}`,
}));

const renderProvider = (language?: string) =>
  render(
    <UserPreferencesContext.Provider value={{ ...defaultUserPreferences, language }}>
      <UserLanguageProvider>content</UserLanguageProvider>
    </UserPreferencesContext.Provider>,
  );

describe('UserLanguageProvider', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  afterEach(() => {
    Store.delete('language');
  });

  it('applies the language of the user preferences', async () => {
    renderProvider('de');

    await screen.findByText('language: de');
  });

  it('falls back to English for users without a language', async () => {
    Store.set('language', 'de');

    renderProvider(undefined);

    await screen.findByText('language: en');
  });

  it('keeps the language chosen in the browser for read-only users', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().readOnly(true).build());
    Store.set('language', 'de');

    renderProvider('en');

    await screen.findByText('language: de');
  });
});
