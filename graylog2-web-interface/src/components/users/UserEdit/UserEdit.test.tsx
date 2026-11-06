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
import React from 'react';
import omit from 'lodash/omit';
import { screen, render, act, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { adminUser, bob } from 'fixtures/users';
import UsersDomain from 'domainActions/users/UsersDomain';
import CurrentUser from 'logic/users/CurrentUser';
import { asMock } from 'helpers/mocking';

import UserEdit from './UserEdit';

jest.mock('./ProfileSection', () => ({ onSubmit }) => (
  <div>
    ProfileSection
    <button onClick={() => onSubmit({ fullName: 'Updated Name' })}>Update Profile</button>
  </div>
));
jest.mock('./PreferencesSection', () => () => <div>PreferencesSection</div>);
jest.mock('./SettingsSection', () => () => <div>SettingsSection</div>);
jest.mock('./PasswordSection', () => () => <div>PasswordSection</div>);
jest.mock('./RolesSection', () => ({ onSubmit }) => (
  <div>
    RolesSection
    <button onClick={() => onSubmit({ roles: ['new-role'] })}>Update Roles</button>
  </div>
));
jest.mock('./TeamsSection', () => () => <div>TeamsSection</div>);
jest.mock('domainActions/users/UsersDomain');
jest.mock('logic/users/CurrentUser');

jest.useFakeTimers();
const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

describe('<UserEdit />', () => {
  afterEach(() => {
    jest.clearAllMocks();
    CurrentUser.reload = jest.fn();
  });

  const user = adminUser.toBuilder().readOnly(false).external(false).build();

  it('should display loading indicator, if no user is provided', async () => {
    render(<UserEdit user={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should not allow editing a readOnly user', () => {
    const readOnlyUser = user.toBuilder().readOnly(true).fullName('Full name').build();
    render(<UserEdit user={readOnlyUser} />);

    expect(screen.getByText(`The selected user ${readOnlyUser.fullName} can't be edited.`)).toBeInTheDocument();
    expect(screen.queryByText('Profile')).not.toBeInTheDocument();
  });

  it('should display profile and password section', () => {
    render(<UserEdit user={user} />);

    expect(screen.getByText('ProfileSection')).toBeInTheDocument();
    expect(screen.getByText('PasswordSection')).toBeInTheDocument();
  });

  it('should display settings and preferences section', async () => {
    render(<UserEdit user={user} />);

    const tab = await screen.findByLabelText(/Preferences/i);
    await setupUser().click(tab);

    expect(screen.getByText('PreferencesSection')).toBeInTheDocument();
    expect(screen.getByText('SettingsSection')).toBeInTheDocument();
  });

  it('should display roles and teams section', async () => {
    render(<UserEdit user={user} />);

    const tab = await screen.findByLabelText(/Teams & Roles/i);
    await setupUser().click(tab);

    expect(screen.getByText('RolesSection')).toBeInTheDocument();
    expect(screen.getByText('TeamsSection')).toBeInTheDocument();
  });

  describe('external user', () => {
    it('should not render profile section for external user', () => {
      render(<UserEdit user={bob} />);

      expect(bob.external).toBeTruthy();
      expect(screen.queryByLabelText('Full Name')).not.toBeInTheDocument();
    });
  });

  it('should send complete UserUpdate object when updating user profile', async () => {
    const updateSpy = jest.fn(() => Promise.resolve());
    asMock(UsersDomain.update).mockImplementation(updateSpy);

    render(<UserEdit user={user} />);

    const updateButton = await screen.findByText('Update Profile');
    await setupUser().click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));

    expect(updateSpy).toHaveBeenCalledWith(
      user.id,
      {
        ...omit(user.toJSON(), ['permissions', 'grn_permissions']),
        fullName: 'Updated Name',
      },
      user.fullName,
    );
  });

  it('should send complete UserUpdate object when updating user roles', async () => {
    const updateSpy = jest.fn(() => Promise.resolve());
    asMock(UsersDomain.update).mockImplementation(updateSpy);

    render(<UserEdit user={user} />);

    const tab = await screen.findByLabelText(/Teams & Roles/i);
    await setupUser().click(tab);

    const updateButton = await screen.findByText('Update Roles');
    await setupUser().click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));

    expect(updateSpy).toHaveBeenCalledWith(
      user.id,
      {
        ...omit(user.toJSON(), ['permissions', 'grn_permissions']),
        roles: ['new-role'],
      },
      user.fullName,
    );
  });

  it('should not send permissions but keep timezone and startpage when updating user', async () => {
    const updateSpy = jest.fn((_userId: string, _payload: object, _fullName: string) => Promise.resolve());
    (UsersDomain.update as jest.Mock) = updateSpy;
    const userWithSettings = user
      .toBuilder()
      .timezone('Europe/Berlin')
      .startpage({ type: 'dashboard', id: 'dashboard-id' })
      .build();

    render(<UserEdit user={userWithSettings} />);

    const updateButton = await screen.findByText('Update Profile');
    await setupUser().click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));

    const payload = updateSpy.mock.calls[0][1];

    expect(payload).not.toHaveProperty('permissions');
    expect(payload).not.toHaveProperty('grn_permissions');
    expect(payload).toHaveProperty('timezone', 'Europe/Berlin');
    expect(payload).toHaveProperty('startpage', { type: 'dashboard', id: 'dashboard-id' });
  });

  it('should reload current user when updating current user', async () => {
    const updateSpy = jest.fn(() => Promise.resolve());
    asMock(UsersDomain.update).mockImplementation(updateSpy);
    const reloadSpy = jest.fn();
    CurrentUser.reload = reloadSpy;

    render(<UserEdit user={user} />);

    const updateButton = await screen.findByText('Update Profile');
    await setupUser().click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(reloadSpy).toHaveBeenCalledTimes(1));
  });

  it('should not reload current user when updating different user', async () => {
    const differentUser = bob.toBuilder().readOnly(false).external(false).build();
    const updateSpy = jest.fn(() => Promise.resolve());
    asMock(UsersDomain.update).mockImplementation(updateSpy);
    const reloadSpy = jest.fn();
    CurrentUser.reload = reloadSpy;

    render(<UserEdit user={differentUser} />);

    const updateButton = await screen.findByText('Update Profile');
    await setupUser().click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));
    expect(reloadSpy).not.toHaveBeenCalled();
  });
});
