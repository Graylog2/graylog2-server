// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { alertsManager as exampleRole } from 'fixtures/roles';
import { alice, bob, charlie } from 'fixtures/userOverviews';
import mockAction from 'helpers/mocking/MockAction';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RoleEdit from './RoleEdit';

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

describe('RoleEdit', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display loading indicator, if no role is provided', async () => {
    render(<RoleEdit role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should display role profile', async () => {
    render(<RoleEdit role={exampleRole} />);

    await screen.findByText(exampleRole.name);

    expect(screen.getByText(exampleRole.description)).toBeInTheDocument();
  });

  it('should assigning a user', async () => {
    render(<RoleEdit role={exampleRole} />);

    const assignUserButton = await screen.findByRole('button', { name: 'Assign User' });
    const usersSelectorBob = screen.getByLabelText('Search for users');
    await selectEvent.openMenu(usersSelectorBob);
    await selectEvent.select(usersSelectorBob, bob.username);

    const userSelectorCharlie = screen.getByLabelText('Search for users');
    await selectEvent.openMenu(userSelectorCharlie);
    await selectEvent.select(userSelectorCharlie, charlie.username);

    fireEvent.click(assignUserButton);

    await waitFor(() => expect(AuthzRolesActions.addMembers).toHaveBeenCalledWith(exampleRole.id, Immutable.Set.of(bob.username, charlie.username)));
  });

  it('should filter assigned users', async () => {
    render(<RoleEdit role={exampleRole} />);
    const filterInput = await screen.findByPlaceholderText('Enter query to filter');
    const filterSubmitButton = screen.getByRole('button', { name: 'Filter' });

    fireEvent.change(filterInput, { target: { value: 'name of an assigned user' } });
    fireEvent.click(filterSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledTimes(2));

    expect(AuthzRolesActions.loadUsersForRole).toHaveBeenCalledWith(exampleRole.id, exampleRole.name, { page: 1, perPage: 5, query: 'name of an assigned user' });
  });

  // it('should unassign a user', async () => {
  //   render(<RoleEdit role={exampleRole} />);

  //   const assignUserButton = await screen.findByRole('button', { name: `Remove ${alice.username}` });
  //   fireEvent.click(assignUserButton);

  //   await waitFor(() => expect(AuthzRolesActions.removeMember).toHaveBeenCalledWith(exampleRole.id, alice.username));
  // });
});
