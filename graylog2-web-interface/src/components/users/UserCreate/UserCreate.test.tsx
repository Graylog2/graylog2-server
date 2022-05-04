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
import * as Immutable from 'immutable';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { act } from 'react-dom/test-utils';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import { alice as existingUser } from 'fixtures/userOverviews';
import { rolesList } from 'fixtures/roles';
import { UsersActions } from 'stores/users/UsersStore';

import UserCreate from './UserCreate';

const mockLoadUsersPromise = Promise.resolve(Immutable.List([existingUser]));
const mockLoadRolesPromise = Promise.resolve({
  list: rolesList,
  pagination: {
    page: 0,
    per_page: 0,
    query: '',
  },
  count: 0,
  total: 0,
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

const extendedTimeout = applyTimeoutMultiplier(15000);

describe('<UserCreate />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should create user', async () => {
    const { findByLabelText, findByPlaceholderText, findByText } = render(<UserCreate />);

    const usernameInput = await findByLabelText('Username');
    const firstNameInput = await findByLabelText('First Name');
    const lastNameInput = await findByLabelText('Last Name');
    const emailInput = await findByLabelText('E-Mail Address');
    const timeoutAmountInput = await findByPlaceholderText('Timeout amount');
    // const timeoutUnitSelect = getByTestId('Timeout unit');
    const timezoneSelect = await findByLabelText('Time Zone');
    const passwordInput = await findByPlaceholderText('Password');
    const passwordRepeatInput = await findByPlaceholderText('Repeat password');
    const submitButton = await findByText('Create User');

    fireEvent.change(usernameInput, { target: { value: 'The username' } });
    fireEvent.change(firstNameInput, { target: { value: 'The first name' } });
    fireEvent.change(lastNameInput, { target: { value: 'The last name' } });
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
      first_name: 'The first name',
      last_name: 'The last name',
      timezone: 'Europe/Berlin',
      roles: ['Reader'],
      email: 'username@example.org',
      permissions: [],
      session_timeout_ms: 144000000,
      password: 'thepassword',
    }));
  }, extendedTimeout);

  it('should trim the username', async () => {
    const { findByLabelText, findByPlaceholderText, findByText } = render(<UserCreate />);

    const usernameInput = await findByLabelText('Username');
    const firstNameInput = await findByLabelText('First Name');
    const lastNameInput = await findByLabelText('Last Name');
    const emailInput = await findByLabelText('E-Mail Address');
    const passwordInput = await findByPlaceholderText('Password');
    const passwordRepeatInput = await findByPlaceholderText('Repeat password');
    const submitButton = await findByText('Create User');

    fireEvent.change(usernameInput, { target: { value: '   username   ' } });
    fireEvent.change(firstNameInput, { target: { value: 'The first name' } });
    fireEvent.change(lastNameInput, { target: { value: 'The last name' } });
    fireEvent.change(emailInput, { target: { value: 'username@example.org' } });
    fireEvent.change(passwordInput, { target: { value: 'thepassword' } });
    fireEvent.change(passwordRepeatInput, { target: { value: 'thepassword' } });

    fireEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.create).toHaveBeenCalledWith({
      username: 'username',
      first_name: 'The first name',
      last_name: 'The last name',
      roles: ['Reader'],
      email: 'username@example.org',
      permissions: [],
      session_timeout_ms: 3600000,
      password: 'thepassword',
    }));
  }, extendedTimeout);

  // The following tests will work when we use @testing-library/user-event instead of fireEvent
  it('should display warning if username is already taken', async () => {
    const { findByLabelText, findByText } = render(<UserCreate />);

    const usernameInput = await findByLabelText('Username');

    await userEvent.type(usernameInput, existingUser.username);

    await findByText(/Username is already taken/);
  }, extendedTimeout);

  it('should display warning, if password repeat does not match password', async () => {
    const { findByPlaceholderText, findByText } = render(<UserCreate />);

    const passwordInput = await findByPlaceholderText('Password');
    const passwordRepeatInput = await findByPlaceholderText('Repeat password');

    await userEvent.type(passwordInput, 'thepassword');
    await userEvent.type(passwordRepeatInput, 'notthepassword');
    fireEvent.blur(passwordRepeatInput);

    await findByText(/Passwords do not match/);
  }, extendedTimeout);
});
