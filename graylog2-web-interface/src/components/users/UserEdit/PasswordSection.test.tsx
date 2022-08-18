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
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { alice, adminUser } from 'fixtures/users';
import { UsersActions } from 'stores/users/UsersStore';

import PasswordSection from './PasswordSection';

const exampleUser = alice;

jest.mock('hooks/useCurrentUser');

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    changePassword: jest.fn(() => Promise.resolve()),
  },
}));

describe('<PasswordSection />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should allow password change', async () => {
    render(<PasswordSection user={exampleUser} />);

    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
    const submitButton = screen.getByText('Change Password');

    fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
    fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledTimes(1));

    expect(UsersActions.changePassword).toHaveBeenCalledWith(exampleUser.id, {
      password: 'newpassword',
    });
  });

  it('should require current password when current user is changing his password', async () => {
    asMock(useCurrentUser).mockReturnValue(exampleUser);
    render(<PasswordSection user={exampleUser} />);

    const passwordInput = screen.getByLabelText('Old Password');
    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
    const submitButton = screen.getByText('Change Password');

    fireEvent.change(passwordInput, { target: { value: 'oldpassword' } });
    fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
    fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledTimes(1));

    expect(UsersActions.changePassword).toHaveBeenCalledWith(exampleUser.id, {
      old_password: 'oldpassword',
      password: 'newpassword',
    });
  });

  it('should display warning, if password repeat does not match password', async () => {
    render(<PasswordSection user={exampleUser} />);

    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');

    fireEvent.change(newPasswordInput, { target: { value: 'thepassword' } });
    fireEvent.change(newPasswordRepeatInput, { target: { value: 'notthepassword' } });
    fireEvent.blur(newPasswordRepeatInput);

    await screen.findByText('Passwords do not match');
  });
});
