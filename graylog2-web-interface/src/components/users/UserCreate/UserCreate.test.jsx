// @flow strict
import React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import { UsersActions } from 'stores/users/UsersStore';

import UserCreate from './UserCreate';

jest.mock('stores/users/UsersStore', () => ({
  UsersStore: {
    getInitialState: jest.fn(() => ({ list: undefined })),
    listen: jest.fn(),
  },
  UsersActions: {
    create: jest.fn(() => Promise.resolve()),
    loadUsers: jest.fn(),
  },
}));

describe('<UserCreate />', () => {
  it('should create user', async () => {
    const { getByLabelText, getByPlaceholderText, getByText } = render(<UserCreate />);

    const usernameInput = getByLabelText('Username');
    const fullNameInput = getByLabelText('Full Name');
    const emailInput = getByLabelText('E-Mail Address');

    const passwordInput = getByPlaceholderText('Password');
    const passwordRepeatInput = getByPlaceholderText('Repeat password');
    const submitButton = getByText('Create User');

    fireEvent.change(usernameInput, { target: { value: 'The username' } });
    fireEvent.change(fullNameInput, { target: { value: 'The full name' } });
    fireEvent.change(emailInput, { target: { value: 'username@example.org' } });
    fireEvent.change(passwordInput, { target: { value: 'thepassword' } });
    fireEvent.change(passwordRepeatInput, { target: { value: 'thepassword' } });

    fireEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.create).toHaveBeenCalledWith({
      username: 'The username',
      full_name: 'The full name',
      email: 'username@example.org',
      permissions: [],
      session_timeout_ms: 3600000,
      password: 'thepassword',
    }));
  });
});
