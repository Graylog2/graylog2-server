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
import { render, waitFor, screen } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';
import { alice } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import { saveUserPreferences } from 'api/preferences';

import PreferencesSection from './PreferencesSection';

jest.mock('api/preferences', () => ({
  saveUserPreferences: jest.fn(() => Promise.resolve()),
}));

const userWithLanguage = (language: string) =>
  alice
    .toBuilder()
    .preferences({ ...alice.preferences, language })
    .build();

describe('<PreferencesSection />', () => {
  it('shows the configured language', async () => {
    render(<PreferencesSection user={userWithLanguage('de')} />);

    await screen.findByText('Deutsch');
  });

  it('saves the selected language', async () => {
    const user = userWithLanguage('de');
    render(<PreferencesSection user={user} />);

    await selectEvent.chooseOption('Language', 'English');
    await userEvent.click(screen.getByRole('button', { name: 'Update Preferences' }));

    await waitFor(() =>
      expect(asMock(saveUserPreferences)).toHaveBeenCalledWith(
        user.username,
        expect.objectContaining({ language: 'en' }),
      ),
    );
  });
});
