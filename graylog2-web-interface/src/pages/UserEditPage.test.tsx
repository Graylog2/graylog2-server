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
import { render, screen, waitFor, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { adminUser } from 'fixtures/users';
import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import { asMock } from 'helpers/mocking';
import type User from 'logic/users/User';

import UserEditPage from './UserEditPage';

type SectionProps = { user: User; onSubmit: (data: object) => Promise<void> };

// Like the real sections (Formik initial values), the section mocks only use the user values they received on mount.
// They also show whether their submit has finished.
const MockSection = ({ name, data, user, onSubmit }: SectionProps & { name: string; data: object }) => {
  const [initialUser] = useState(user);
  const [saved, setSaved] = useState(false);

  return (
    <div>
      <span>
        {name} last name: {initialUser.lastName}
      </span>
      {saved && <span>{name} saved</span>}
      <button type="button" onClick={() => onSubmit(data).then(() => setSaved(true))}>
        Update {name}
      </button>
    </div>
  );
};

jest.mock('components/users/UserEdit/ProfileSection', () => (props: SectionProps) => (
  <MockSection name="Profile" data={{ last_name: 'Updated' }} {...props} />
));
jest.mock('components/users/UserEdit/SettingsSection', () => (props: SectionProps) => (
  <MockSection name="Settings" data={{ timezone: 'America/Vancouver' }} {...props} />
));
jest.mock('components/users/UserEdit/PreferencesSection', () => () => <div>PreferencesSection</div>);
jest.mock('components/users/UserEdit/PasswordSection', () => () => <div>PasswordSection</div>);
jest.mock('components/users/UserEdit/RolesSection', () => () => <div>RolesSection</div>);
jest.mock('components/users/UserEdit/TeamsSection', () => () => <div>TeamsSection</div>);
jest.mock('components/users/navigation/UsersPageNavigation', () => () => <div>UsersPageNavigation</div>);
jest.mock('components/users/navigation/UserActionLinks', () => () => <div>UserActionLinks</div>);
jest.mock('domainActions/users/UsersDomain');
jest.mock('stores/users/CurrentUserStore');
jest.mock('routing/withParams', () => (x) => x);

const user = adminUser
  .toBuilder()
  .readOnly(false)
  .external(false)
  .lastName('Original')
  .timezone('Europe/Berlin')
  .build();
const updatedUser = user.toBuilder().lastName('Updated').timezone('America/Vancouver').build();

const SimpleUserEditPage = () => (
  // `withParams` is mocked, so the page expects the params as prop.
  // @ts-expect-error
  <UserEditPage params={{ userId: user.id }} />
);

const deferredLoad = () => {
  let resolveLoad: (u: User) => void;
  const promise = new Promise<User>((resolve) => {
    resolveLoad = resolve;
  });

  return { promise, resolve: (u: User) => resolveLoad(u) };
};

// The page updates its user when `UsersActions.load` completes. The mocked `UsersDomain.load` doesn't trigger the
// action, so we complete it ourselves, like the real `UsersDomain.load` does.
const loadAndComplete = (result: User | Promise<User>) => () =>
  Promise.resolve(result).then((loaded) => {
    UsersActions.load.completed(loaded);

    return loaded;
  });

const mockLoads = (initial: User, reload: User | Promise<User>) =>
  asMock(UsersDomain.load).mockImplementationOnce(loadAndComplete(initial)).mockImplementation(loadAndComplete(reload));

const selectTab = (name: RegExp) => screen.findByLabelText(name).then((tab) => userEvent.click(tab));

describe('UserEditPage', () => {
  beforeEach(() => {
    asMock(UsersDomain.update).mockResolvedValue(undefined);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows saved values after switching sections', async () => {
    mockLoads(user, updatedUser);

    render(<SimpleUserEditPage />);

    await screen.findByText('Profile last name: Original');
    await userEvent.click(screen.getByRole('button', { name: 'Update Profile' }));
    await screen.findByText('Profile saved');

    await selectTab(/Preferences/i);
    await selectTab(/Profile/i);

    await screen.findByText('Profile last name: Updated');
  });

  it('sends the saved timezone with following updates of other sections', async () => {
    mockLoads(user, updatedUser);

    render(<SimpleUserEditPage />);

    await selectTab(/Preferences/i);
    await userEvent.click(await screen.findByRole('button', { name: 'Update Settings' }));
    await screen.findByText('Settings saved');

    await selectTab(/Profile/i);
    await userEvent.click(await screen.findByRole('button', { name: 'Update Profile' }));

    await waitFor(() => expect(UsersDomain.update).toHaveBeenCalledTimes(2));

    expect(UsersDomain.update).toHaveBeenLastCalledWith(
      user.id,
      expect.objectContaining({ timezone: 'America/Vancouver', last_name: 'Updated' }),
      user.fullName,
    );
  });

  it('finishes the update and allows switching sections only after the user has been reloaded', async () => {
    const reload = deferredLoad();
    mockLoads(user, reload.promise);

    render(<SimpleUserEditPage />);

    await userEvent.click(await screen.findByRole('button', { name: 'Update Profile' }));

    await waitFor(() => expect(UsersDomain.load).toHaveBeenCalledTimes(2));

    expect(screen.queryByText('Profile saved')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Preferences/i)).toBeDisabled();

    await act(async () => reload.resolve(updatedUser));

    await screen.findByText('Profile saved');

    expect(screen.getByLabelText(/Preferences/i)).toBeEnabled();
  });
});
