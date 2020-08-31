// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { alertsManager } from 'fixtures/roles';
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
    loadUsersForRole: jest.fn(() => mockLoadUsersForRolePromise),
    addMember: mockAction(jest.fn(() => Promise.resolve())),
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
  UsersStore: {},
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.useFakeTimers();

describe('RoleEdit', () => {
  it('should display loading indicator, if no role is provided', async () => {
    const { queryByText } = render(<RoleEdit role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(queryByText('Loading...')).not.toBeNull();
  });

  it('should display role profile', async () => {
    const { queryByText } = render(<RoleEdit role={alertsManager} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    expect(queryByText(alertsManager.name)).not.toBeNull();
    expect(queryByText(alertsManager.description)).not.toBeNull();
  });

  it('should assigning a user', async () => {
    const { getByLabelText, getByRole } = render(<RoleEdit role={alertsManager} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const assignUserButton = getByRole('button', { name: 'Assign User' });
    const usersSelector = getByLabelText('Search for users');
    await selectEvent.openMenu(usersSelector);
    await selectEvent.select(usersSelector, bob.username);
    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.addMember).toHaveBeenCalledWith(alertsManager.id, bob.username));
  });

  it('should filter assigned users', async () => {
    const { getByPlaceholderText, getByRole } = render(<RoleEdit role={alertsManager} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);
    const filterInput = getByPlaceholderText('Enter query to filter');
    const filterSubmitButton = getByRole('button', { name: 'Filter' });

    fireEvent.change(filterInput, { target: { value: 'name of an assignd user' } });
    fireEvent.click(filterSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledWith(alertsManager.id, alertsManager.name, 1, 10, 'name of an assignd user'));
  });

  it('should unassigning a user', async () => {
    const { getByRole } = render(<RoleEdit role={alertsManager} />);
    await act(() => mockLoadUsersForRolePromise);
    await act(() => mockLoadUsersPromise);

    const assignUserButton = getByRole('button', { name: `Remove ${alice.username}` });
    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.removeMember).toHaveBeenCalledWith(alertsManager.id, alice.username));
  });
});
