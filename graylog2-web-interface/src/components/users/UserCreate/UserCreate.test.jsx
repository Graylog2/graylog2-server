// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { act } from 'react-dom/test-utils';
import { alice } from 'fixtures/userOverviews';
import { rolesList } from 'fixtures/roles';

import { UsersActions } from 'stores/users/UsersStore';

import UserCreate from './UserCreate';

const existingUser = alice;
const mockLoadUsersPromise = Promise.resolve(Immutable.List([existingUser]));
const mockLoadRolesPromise = Promise.resolve({
  list: rolesList,
  pagination: {
    count: 0,
    total: 0,
    page: 0,
    per_page: 0,
    query: '',
  },
});

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    create: jest.fn(() => Promise.resolve()),
    loadUsers: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesPaginated: jest.fn(() => Promise.resolve(mockLoadRolesPromise)),
  },
}));

jest.setTimeout(10000);

describe('<UserCreate />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should create user', async () => {
    const { findByLabelText, findByPlaceholderText, findByText } = render(<UserCreate />);

    const usernameInput = await findByLabelText('Username');
    const fullNameInput = await findByLabelText('Full Name');
    const emailInput = await findByLabelText('E-Mail Address');
    const timeoutAmountInput = await findByPlaceholderText('Timeout amount');
    // const timeoutUnitSelect = getByTestId('Timeout unit');
    const timezoneSelect = await findByLabelText('Time Zone');
    const passwordInput = await findByPlaceholderText('Password');
    const passwordRepeatInput = await findByPlaceholderText('Repeat password');
    const submitButton = await findByText('Create User');

    fireEvent.change(usernameInput, { target: { value: 'The username' } });
    fireEvent.change(fullNameInput, { target: { value: 'The full name' } });
    fireEvent.change(emailInput, { target: { value: 'username@example.org' } });
    fireEvent.change(timeoutAmountInput, { target: { value: '40' } });
    // await selectEvent.openMenu(timeoutUnitSelect);
    // await act(async () => { await selectEvent.select(timeoutUnitSelect, 'Seconds'); });
    await selectEvent.openMenu(timezoneSelect);
    await act(async () => { await selectEvent.select(timezoneSelect, 'Berlin'); });
    fireEvent.change(passwordInput, { target: { value: 'thepassword' } });
    fireEvent.change(passwordRepeatInput, { target: { value: 'thepassword' } });

    fireEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.create).toHaveBeenCalledWith({
      username: 'The username',
      full_name: 'The full name',
      timezone: 'Europe/Berlin',
      roles: ['Reader'],
      email: 'username@example.org',
      permissions: [],
      session_timeout_ms: 144000000,
      password: 'thepassword',
    }));
  });

  // The following tests will work when we use @testing-library/user-event instead of fireEvent
  // it('should display warning if username is already taken', async () => {
  //   const { findByLabelText, findByText } = render(<UserCreate />);

  //   const usernameInput = await findByLabelText('Username');

  //   fireEvent.change(usernameInput, { target: { value: existingUser.username } });

  //   await findByText('Username is already taken');
  // });

  // it('should display warning, if password repeat does not match password', async () => {
  //   const { findByPlaceholderText, findByText } = render(<UserCreate />);

  //   const passwordInput = await findByPlaceholderText('Password');
  //   const passwordRepeatInput = await findByPlaceholderText('Repeat password');

  //   fireEvent.change(passwordInput, { target: { value: 'thepassword' } });
  //   fireEvent.change(passwordRepeatInput, { target: { value: 'notthepassword' } });

  //   await findByText('Passwords do not match');
  // });
});
