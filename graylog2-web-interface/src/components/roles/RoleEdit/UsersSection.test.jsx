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
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { alertsManager as exampleRole } from 'fixtures/roles';
import { alice, bob, charlie } from 'fixtures/userOverviews';
import mockAction from 'helpers/mocking/MockAction';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import UsersSection from './UsersSection';

const mockLoadUsersForRolePromise = Promise.resolve({
  list: Immutable.List([alice]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
});

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesStore: {},
  AuthzRolesActions: {
    removeMember: mockAction(jest.fn(() => Promise.resolve())),
    addMembers: mockAction(jest.fn(() => Promise.resolve())),
    loadUsersForRole: jest.fn(() => mockLoadUsersForRolePromise),
  },
}));

// mock loadUsersPaginated
const mockLoadUsersPromise = Promise.resolve({
  list: Immutable.List([bob, charlie]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
});

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.useFakeTimers();

describe('UsersSection', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should assigning a user', async () => {
    render(<UsersSection role={exampleRole} />);
    await act(() => mockLoadUsersPromise);
    await act(() => mockLoadUsersForRolePromise);

    const assignUserButton = screen.getByRole('button', { name: 'Assign User' });
    const usersSelector = screen.getByLabelText('Search for users');
    await selectEvent.select(usersSelector, bob.username);

    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.addMembers).toHaveBeenCalledTimes(1));

    expect(AuthzRolesActions.addMembers).toHaveBeenCalledWith(exampleRole.id, Immutable.Set.of(bob.username));
  });

  it('should filter assigned users', async () => {
    render(<UsersSection role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const filterInput = screen.getByPlaceholderText('Enter query to filter');
    const filterSubmitButton = screen.getByRole('button', { name: 'Filter' });

    fireEvent.change(filterInput, { target: { value: 'name of an assigned user' } });
    fireEvent.click(filterSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledTimes(2));

    expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledWith(exampleRole.id, exampleRole.name, { page: 1, perPage: 5, query: 'name of an assigned user' });
  });

  it('should unassign a user', async () => {
    render(<UsersSection role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const assignUserButton = await screen.findByRole('button', { name: `Remove ${alice.username}` });
    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.removeMember).toHaveBeenCalledWith(exampleRole.id, alice.username));
  });
});
