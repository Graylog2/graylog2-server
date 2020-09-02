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
const mockLoadUsersPromise = Promise.resolve(Immutable.List([alice]));
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
    const { getByLabelText, getByPlaceholderText, getByText } = render(<UserCreate />);

    await act(() => mockLoadUsersPromise);
    await act(() => mockLoadRolesPromise);

    const usernameInput = getByLabelText('Username');
    const fullNameInput = getByLabelText('Full Name');
    const emailInput = getByLabelText('E-Mail Address');
    const timeoutAmountInput = getByPlaceholderText('Timeout amount');
    // const timeoutUnitSelect = getByTestId('Timeout unit');
    const timezoneSelect = getByLabelText('Time Zone');
    const passwordInput = getByPlaceholderText('Password');
    const passwordRepeatInput = getByPlaceholderText('Repeat password');
    const submitButton = getByText('Create User');

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

  it('should display warning if username is alreafy taken', async () => {
    const { getByLabelText, getByText } = render(<UserCreate />);

    await act(() => mockLoadUsersPromise);
    await act(() => mockLoadRolesPromise);

    const usernameInput = getByLabelText('Username');

    fireEvent.change(usernameInput, { target: { value: existingUser.username } });

    await waitFor(() => expect(getByText('Username is already taken')).not.toBeNull());
  });

  it('should display warning, if password repeat does not match password', async () => {
    const { getByPlaceholderText, getByText } = render(<UserCreate />);

    await act(() => mockLoadUsersPromise);
    await act(() => mockLoadRolesPromise);

    const passwordInput = getByPlaceholderText('Password');
    const passwordRepeatInput = getByPlaceholderText('Repeat password');

    fireEvent.change(passwordInput, { target: { value: 'thepassword' } });
    fireEvent.change(passwordRepeatInput, { target: { value: 'notthepassword' } });

    await waitFor(() => expect(getByText('Passwords do not match')).not.toBeNull());
  });
});
