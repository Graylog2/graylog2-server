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
import { useState } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { adminUser } from 'fixtures/users';
import UsersDomain from 'domainActions/users/UsersDomain';
import { saveUserPreferences } from 'api/preferences';
import { asMock } from 'helpers/mocking';
import type User from 'logic/users/User';

import UserEditPage from './UserEditPage';

type SectionProps = { user: User; onSubmit: (data: object) => Promise<void> };

// Like the real section (Formik initial values), the mock only uses the user values it received on mount.
// It also shows whether its submit has finished.
const MockPreferencesSection = ({ user, onSubmit }: SectionProps) => {
  const [initialUser] = useState(user);
  const [saved, setSaved] = useState(false);

  return (
    <div>
      <span>Preferences update unfocussed: {String(initialUser.preferences.updateUnfocussed)}</span>
      {saved && <span>Preferences saved</span>}
      <button type="button" onClick={() => onSubmit({ updateUnfocussed: true }).then(() => setSaved(true))}>
        Update Preferences
      </button>
    </div>
  );
};

jest.mock('components/users/UserEdit/PreferencesSection', () => (props: SectionProps) => (
  <MockPreferencesSection {...props} />
));
jest.mock('components/users/UserEdit/ProfileSection', () => () => <div>ProfileSection</div>);
jest.mock('components/users/UserEdit/SettingsSection', () => () => <div>SettingsSection</div>);
jest.mock('components/users/UserEdit/PasswordSection', () => () => <div>PasswordSection</div>);
jest.mock('components/users/UserEdit/RolesSection', () => () => <div>RolesSection</div>);
jest.mock('components/users/UserEdit/TeamsSection', () => () => <div>TeamsSection</div>);
jest.mock('components/users/navigation/UsersPageNavigation', () => () => <div>UsersPageNavigation</div>);
jest.mock('components/users/navigation/UserActionLinks', () => () => <div>UserActionLinks</div>);
jest.mock('domainActions/users/UsersDomain');
jest.mock('api/preferences', () => ({ saveUserPreferences: jest.fn() }));
jest.mock('logic/users/CurrentUser');
jest.mock('routing/withParams', () => (x) => x);

const user = adminUser
  .toBuilder()
  .readOnly(false)
  .external(false)
  .preferences({ enableSmartSearch: true, updateUnfocussed: false, themeMode: 'dark' })
  .build();

const SimpleUserEditPage = () => (
  // `withParams` is mocked, so the page expects the params as prop.
  // @ts-expect-error
  <UserEditPage params={{ userId: user.id }} />
);

const selectTab = (name: RegExp) => screen.findByLabelText(name).then((tab) => userEvent.click(tab));

describe('UserEditPage', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows saved preferences after switching sections', async () => {
    const userWithUpdatedPreferences = user
      .toBuilder()
      .preferences({ ...user.preferences, updateUnfocussed: true })
      .build();
    asMock(UsersDomain.load).mockResolvedValueOnce(user).mockResolvedValue(userWithUpdatedPreferences);
    asMock(saveUserPreferences).mockResolvedValue(undefined);

    render(<SimpleUserEditPage />);

    await selectTab(/Preferences/i);
    await screen.findByText('Preferences update unfocussed: false');
    await userEvent.click(screen.getByRole('button', { name: 'Update Preferences' }));
    await screen.findByText('Preferences saved');

    expect(saveUserPreferences).toHaveBeenCalledWith(user.username, { updateUnfocussed: true });

    await selectTab(/Profile/i);
    await selectTab(/Preferences/i);

    await screen.findByText('Preferences update unfocussed: true');
  });
});
