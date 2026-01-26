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
import { screen, render, act, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { adminUser, bob } from 'fixtures/users';
import UsersDomain from 'domainActions/users/UsersDomain';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

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
jest.mock('stores/users/CurrentUserStore');

jest.useFakeTimers();

describe('<UserEdit />', () => {
  afterEach(() => {
    jest.clearAllMocks();
    CurrentUserStore.reload = jest.fn();
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
    userEvent.click(tab);

    expect(screen.getByText('PreferencesSection')).toBeInTheDocument();
    expect(screen.getByText('SettingsSection')).toBeInTheDocument();
  });

  it('should display roles and teams section', async () => {
    render(<UserEdit user={user} />);

    const tab = await screen.findByLabelText(/Teams & Roles/i);
    userEvent.click(tab);

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
    (UsersDomain.update as jest.Mock) = updateSpy;

    render(<UserEdit user={user} />);

    const updateButton = await screen.findByText('Update Profile');
    await userEvent.click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));

    expect(updateSpy).toHaveBeenCalledWith(
      user.id,
      {
        ...user.toJSON(),
        fullName: 'Updated Name',
      },
      user.fullName,
    );
  });

  it('should send complete UserUpdate object when updating user roles', async () => {
    const updateSpy = jest.fn(() => Promise.resolve());
    (UsersDomain.update as jest.Mock) = updateSpy;

    render(<UserEdit user={user} />);

    const tab = await screen.findByLabelText(/Teams & Roles/i);
    await userEvent.click(tab);

    const updateButton = await screen.findByText('Update Roles');
    await userEvent.click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));

    expect(updateSpy).toHaveBeenCalledWith(
      user.id,
      {
        ...user.toJSON(),
        roles: ['new-role'],
      },
      user.fullName,
    );
  });

  it('should reload CurrentUserStore when updating current user', async () => {
    const updateSpy = jest.fn(() => Promise.resolve());
    (UsersDomain.update as jest.Mock) = updateSpy;
    const reloadSpy = jest.fn();
    CurrentUserStore.reload = reloadSpy;

    render(<UserEdit user={user} />);

    const updateButton = await screen.findByText('Update Profile');
    await userEvent.click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(reloadSpy).toHaveBeenCalledTimes(1));
  });

  it('should not reload CurrentUserStore when updating different user', async () => {
    const differentUser = bob.toBuilder().readOnly(false).external(false).build();
    const updateSpy = jest.fn(() => Promise.resolve());
    (UsersDomain.update as jest.Mock) = updateSpy;
    const reloadSpy = jest.fn();
    CurrentUserStore.reload = reloadSpy;

    render(<UserEdit user={differentUser} />);

    const updateButton = await screen.findByText('Update Profile');
    await userEvent.click(updateButton);

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(1));
    expect(reloadSpy).not.toHaveBeenCalled();
  });
});
