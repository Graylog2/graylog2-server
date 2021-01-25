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
import * as Immutable from 'immutable';
import selectEvent from 'react-select-event';
import { render, fireEvent, waitFor, screen, act } from 'wrappedTestingLibrary';
import { alice } from 'fixtures/users';
import { manager as assignedRole1, reader as assignedRole2, reportCreator as notAssignedRole } from 'fixtures/roles';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RolesSection from './RolesSection';

const exampleUser = alice.toBuilder()
  .roles(Immutable.Set([assignedRole1.name]))
  .build();
const mockRolesForUserPromise = Promise.resolve({ list: Immutable.List([assignedRole1]), pagination: { page: 1, perPage: 10, total: 1 } });
const mockLoadRolesPromise = Promise.resolve({ list: Immutable.List([notAssignedRole]), pagination: { page: 1, perPage: 10, total: 1 } });

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesForUser: jest.fn(() => mockRolesForUserPromise),
    loadRolesPaginated: jest.fn(() => mockLoadRolesPromise),
  },
}));

describe('<RolesSection />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should assigning a role', async () => {
    const onSubmitStub = jest.fn((data: { roles: string[]}) => Promise.resolve());
    render(<RolesSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);
    await act(() => mockRolesForUserPromise);
    await act(() => mockLoadRolesPromise);

    const assignRoleButton = screen.getByRole('button', { name: 'Assign Role' });
    const rolesSelector = screen.getByLabelText('Search for roles');
    await selectEvent.openMenu(rolesSelector);
    await selectEvent.select(rolesSelector, notAssignedRole.name);
    fireEvent.click(assignRoleButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({ roles: [assignedRole1.name, notAssignedRole.name] });
  });

  it('should filter assigned roles', async () => {
    const onSubmitStub = jest.fn((data: { roles: string[]}) => Promise.resolve());
    render(<RolesSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);
    await act(() => mockRolesForUserPromise);
    await act(() => mockLoadRolesPromise);

    const filterInput = screen.getByPlaceholderText('Enter query to filter');
    const filterSubmitButton = screen.getByRole('button', { name: 'Filter' });

    fireEvent.change(filterInput, { target: { value: 'name of an assigned role' } });
    fireEvent.click(filterSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesForUser).toHaveBeenCalledTimes(2));

    expect(AuthzRolesActions.loadRolesForUser).toHaveBeenCalledWith(exampleUser.username, { page: 1, perPage: 5, query: 'name of an assigned role' });
  });

  it('should unassign a role', async () => {
    const newExampleUser = alice.toBuilder()
      .roles(Immutable.Set([assignedRole1.name, assignedRole2.name]))
      .build();
    const onSubmitStub = jest.fn((data: { roles: string[]}) => Promise.resolve());
    render(<RolesSection user={newExampleUser} onSubmit={(data) => onSubmitStub(data)} />);
    await act(() => mockRolesForUserPromise);
    await act(() => mockLoadRolesPromise);

    const unassignRoleButton = screen.getByRole('button', { name: `Remove ${assignedRole1.name}` });
    fireEvent.click(unassignRoleButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({ roles: [assignedRole2.name] });
  });
});
