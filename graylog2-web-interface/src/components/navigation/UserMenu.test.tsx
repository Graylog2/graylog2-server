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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import UserPreferencesContext, { defaultUserPreferences } from 'contexts/UserPreferencesContext';

import UserMenu from './UserMenu';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useLogout', () => () => jest.fn());

const renderUserMenu = (language?: string) =>
  render(
    <UserPreferencesContext.Provider value={{ ...defaultUserPreferences, language }}>
      <UserMenu />
    </UserPreferencesContext.Provider>,
  );

describe('<UserMenu />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('shows the current language', async () => {
    renderUserMenu('de');

    await userEvent.click(screen.getByTitle(`User Menu for ${defaultUser.fullName}`));

    await screen.findByRole('menuitem', { name: /language.*deutsch/i });
  });

  it('falls back to English when no language is configured', async () => {
    renderUserMenu(undefined);

    await userEvent.click(screen.getByTitle(`User Menu for ${defaultUser.fullName}`));

    await screen.findByRole('menuitem', { name: /language.*english/i });
  });
});
