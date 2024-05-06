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
import { render, waitFor, screen, act } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import { alice as existingUser } from 'fixtures/users';
import { rolesList } from 'fixtures/roles';
import { UsersActions } from 'stores/users/UsersStore';
import { asMock } from 'helpers/mocking';
import FetchError from 'logic/errors/FetchError';

import UserCreate from './UserCreate';

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
    loadByUsername: jest.fn(),
  },
}));

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesPaginated: jest.fn(() => Promise.resolve(mockLoadRolesPromise)),
  },
}));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

const extendedTimeout = applyTimeoutMultiplier(15000);

describe('<UserCreate />', () => {
  const findSubmitButton = () => screen.findByRole('button', { name: /create user/i });

  beforeEach(() => {
    asMock(UsersActions.loadByUsername).mockImplementation(() => Promise.reject(new FetchError('', 404, {})));
  });

  it('should create user', async () => {
    render(<UserCreate />);

    const usernameInput = await screen.findByLabelText('Username');
    const firstNameInput = await screen.findByLabelText('First Name');
    const lastNameInput = await screen.findByLabelText('Last Name');
    const emailInput = await screen.findByLabelText('E-Mail Address');
    const timeoutAmountInput = await screen.findByPlaceholderText('Timeout amount');
    const timezoneSelect = await screen.findByLabelText('Time Zone');
    const roleSelect = await screen.findByText(/search for roles/i);
    const passwordInput = await screen.findByPlaceholderText('Password');
    const passwordRepeatInput = await screen.findByPlaceholderText('Repeat password');
    const submitButton = await findSubmitButton();
    await userEvent.type(usernameInput, 'The username');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(firstNameInput, 'The first name');
    });

    await userEvent.type(lastNameInput, 'The last name');
    await userEvent.type(emailInput, 'username@example.org');
    await userEvent.clear(timeoutAmountInput);
    await userEvent.type(timeoutAmountInput, '40');

    await act(async () => {
      await selectEvent.openMenu(timezoneSelect);
    });

    await act(async () => {
      await selectEvent.select(timezoneSelect, 'Berlin');
    });

    await act(async () => {
      await selectEvent.openMenu(roleSelect);
    });

    await act(async () => {
      await selectEvent.select(roleSelect, 'Manager');
    });

    await userEvent.type(passwordInput, 'thepassword');
    await userEvent.type(passwordRepeatInput, 'thepassword');

    await waitFor(() => expect(submitButton).toBeEnabled());
    await userEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.create).toHaveBeenCalledWith({
      username: 'The username',
      first_name: 'The first name',
      last_name: 'The last name',
      timezone: 'Europe/Berlin',
      roles: ['Reader', 'Manager'],
      email: 'username@example.org',
      permissions: [],
      session_timeout_ms: 144000000,
      password: 'thepassword',
    }));
  }, extendedTimeout);

  it('should trim the username', async () => {
    render(<UserCreate />);

    const usernameInput = await screen.findByLabelText('Username');
    const firstNameInput = await screen.findByLabelText('First Name');
    const lastNameInput = await screen.findByLabelText('Last Name');
    const emailInput = await screen.findByLabelText('E-Mail Address');
    const passwordInput = await screen.findByPlaceholderText('Password');
    const passwordRepeatInput = await screen.findByPlaceholderText('Repeat password');
    const submitButton = await findSubmitButton();

    await userEvent.type(usernameInput, '   username   ');
    await userEvent.type(firstNameInput, 'The first name');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(lastNameInput, 'The last name');
    });

    await userEvent.type(emailInput, 'username@example.org');
    await userEvent.type(passwordInput, 'thepassword');
    await userEvent.type(passwordRepeatInput, 'thepassword');

    await waitFor(() => expect(submitButton).toBeEnabled());
    await userEvent.click(submitButton);

    await waitFor(() => expect(UsersActions.create).toHaveBeenCalledWith({
      username: 'username',
      first_name: 'The first name',
      last_name: 'The last name',
      roles: ['Reader'],
      email: 'username@example.org',
      permissions: [],
      password: 'thepassword',
    }));
  }, extendedTimeout);

  it('should display warning if username is already taken', async () => {
    asMock(UsersActions.loadByUsername).mockReturnValue(Promise.resolve(existingUser));

    render(<UserCreate />);

    const usernameInput = await screen.findByLabelText('Username');

    await userEvent.type(usernameInput, existingUser.username);

    await userEvent.tab();
    await screen.findByText(/Username is already taken/);
  }, extendedTimeout);

  it('should display warning, if password repeat does not match password', async () => {
    render(<UserCreate />);

    const passwordInput = await screen.findByPlaceholderText('Password');
    const passwordRepeatInput = await screen.findByPlaceholderText('Repeat password');

    await userEvent.type(passwordInput, 'thepassword');
    await userEvent.type(passwordRepeatInput, 'notthepassword');
    await userEvent.tab();

    await screen.findByText(/Passwords do not match/);
  }, extendedTimeout);
});
