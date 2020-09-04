// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { alertsManager as exampleRole } from 'fixtures/roles';
import { alice, bob } from 'fixtures/userOverviews';
import mockAction from 'helpers/mocking/MockAction';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RoleEdit from './RoleEdit';

// mock loadUsersForRole
const paginatedUsersForRole = {
  list: Immutable.List([alice]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
};
const mockLoadUsersForRolePromise = Promise.resolve(paginatedUsersForRole);

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesStore: {},
  AuthzRolesActions: {
    removeMember: mockAction(jest.fn(() => Promise.resolve())),
    addMember: mockAction(jest.fn(() => Promise.resolve())),
    loadUsersForRole: jest.fn(() => mockLoadUsersForRolePromise),
  },
}));

// mock loadUsersPaginated
const paginatedUsers = {
  list: Immutable.List([bob]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
};
const mockLoadUsersPromise = Promise.resolve(paginatedUsers);

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.useFakeTimers();

describe('RoleEdit', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display loading indicator, if no role is provided', async () => {
    const { queryByText } = render(<RoleEdit role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(queryByText('Loading...')).not.toBeNull();
  });

  it('should display role profile', async () => {
    const { queryByText } = render(<RoleEdit role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    expect(queryByText(exampleRole.name)).not.toBeNull();
    expect(queryByText(exampleRole.description)).not.toBeNull();
  });

  it('should assigning a user', async () => {
    const { getByLabelText, getByRole } = render(<RoleEdit role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const assignUserButton = getByRole('button', { name: 'Assign User' });
    const usersSelector = getByLabelText('Search for users');
    await selectEvent.openMenu(usersSelector);
    await selectEvent.select(usersSelector, bob.username);
    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.addMember).toHaveBeenCalledWith(exampleRole.id, bob.username));
  });

  it('should filter assigned users', async () => {
    const { getByPlaceholderText, getByRole } = render(<RoleEdit role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);
    const filterInput = getByPlaceholderText('Enter query to filter');
    const filterSubmitButton = getByRole('button', { name: 'Filter' });

    fireEvent.change(filterInput, { target: { value: 'name of an assigned user' } });
    fireEvent.click(filterSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledWith(exampleRole.id, exampleRole.name, 1, 10, 'name of an assigned user'));
  });

  it('should unassigning a user', async () => {
    const { getByRole } = render(<RoleEdit role={exampleRole} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const assignUserButton = getByRole('button', { name: `Remove ${alice.username}` });
    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.removeMember).toHaveBeenCalledWith(exampleRole.id, alice.username));
  });
});
