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
import { render, waitFor, screen, act } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { alice } from 'fixtures/users';
import usePasswordComplexityConfig from 'components/users/usePasswordComplexityConfig';
import { PASSWORD_SPECIAL_CHARACTERS } from 'logic/users/passwordComplexity';
import { UsersActions } from 'stores/users/UsersStore';

import PasswordSection from './PasswordSection';

const exampleUser = alice;
const passwordComplexityConfig = {
  min_length: 8,
  require_uppercase: true,
  require_lowercase: true,
  require_numbers: true,
  require_special_chars: true,
};

jest.mock('hooks/useCurrentUser');
jest.mock('components/users/usePasswordComplexityConfig');

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    changePassword: jest.fn(() => Promise.resolve()),
  },
}));

describe('<PasswordSection />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(usePasswordComplexityConfig).mockReturnValue(passwordComplexityConfig);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should allow password change', async () => {
    render(<PasswordSection user={exampleUser} />);

    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
    const submitButton = screen.getByText('Change Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(newPasswordInput, 'Abcdef1!');
      await userEvent.type(newPasswordRepeatInput, 'Abcdef1!');
      await userEvent.click(submitButton);
    });

    await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledTimes(1));

    expect(UsersActions.changePassword).toHaveBeenCalledWith(exampleUser.id, {
      password: 'Abcdef1!',
    });
  });

  it('should require current password when current user is changing his password', async () => {
    asMock(useCurrentUser).mockReturnValue(exampleUser);
    render(<PasswordSection user={exampleUser} />);

    const passwordInput = screen.getByLabelText('Old Password');
    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
    const submitButton = screen.getByText('Change Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(passwordInput, 'oldpassword');
      await userEvent.type(newPasswordInput, 'Abcdef1!');
      await userEvent.type(newPasswordRepeatInput, 'Abcdef1!');
      await userEvent.click(submitButton);
    });

    await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledTimes(1));

    expect(UsersActions.changePassword).toHaveBeenCalledWith(exampleUser.id, {
      old_password: 'oldpassword',
      password: 'Abcdef1!',
    });
  });

  it('should display warning, if password repeat does not match password', async () => {
    render(<PasswordSection user={exampleUser} />);

    const newPasswordInput = screen.getByLabelText('New Password');
    const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(newPasswordInput, 'Abcdef1!');
      await userEvent.type(newPasswordRepeatInput, 'Abcdef1?');
      await userEvent.tab();
    });

    await screen.findByText('Passwords do not match');
  });

  it('hides the helper once password meets requirements', async () => {
    render(<PasswordSection user={exampleUser} />);

    expect(screen.getByText('Password must be at least 8 characters long.', { exact: false })).toBeInTheDocument();

    const newPasswordInput = screen.getByLabelText('New Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(newPasswordInput, 'Abcdef1!');
    });

    await waitFor(() => {
      expect(
        screen.queryByText('Password must be at least 8 characters long.', { exact: false }),
      ).not.toBeInTheDocument();
    });
  });

  it('shows only unmet rules when password is invalid', async () => {
    render(<PasswordSection user={exampleUser} />);

    const newPasswordInput = screen.getByLabelText('New Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(newPasswordInput, 'abc');
      await userEvent.tab();
    });

    expect(screen.getByText('Password must be at least 8 characters long.', { exact: false })).toBeInTheDocument();
    expect(
      screen.getByText('Password must contain at least one uppercase letter.', { exact: false }),
    ).toBeInTheDocument();
    expect(screen.getByText('Password must contain at least one number.', { exact: false })).toBeInTheDocument();
    expect(
      screen.getByText(`Password must contain at least one special character from: ${PASSWORD_SPECIAL_CHARACTERS}`, {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Password must contain at least one lowercase letter.', { exact: false }),
    ).not.toBeInTheDocument();
  });
});
