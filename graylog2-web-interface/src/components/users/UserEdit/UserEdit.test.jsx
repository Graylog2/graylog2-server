// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import { screen, render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { reader as assignedRole, reportCreator as notAssignedRole } from 'fixtures/roles';
import { admin as currentUser } from 'fixtures/users';

// import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { UsersActions } from 'stores/users/UsersStore';
import User from 'logic/users/User';

import UserEdit from './UserEdit';

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    update: jest.fn(() => Promise.resolve()),
    load: jest.fn(() => Promise.resolve()),
    changePassword: jest.fn(() => Promise.resolve()),
  },
}));

const mockRolesForUserPromise = Promise.resolve({ list: Immutable.List([assignedRole]), pagination: { page: 1, perPage: 10, total: 1 } });
const mockLoadRolesPromise = Promise.resolve({ list: Immutable.List([notAssignedRole]), pagination: { page: 1, perPage: 10, total: 1 } });
const user = User
  .builder()
  .id('user-id')
  .fullName('The full name')
  .username('The username')
  .roles(Immutable.Set([assignedRole.name]))
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesForUser: jest.fn(() => mockRolesForUserPromise),
    loadRolesPaginated: jest.fn(() => mockLoadRolesPromise),
  },
}));

describe('<UserEdit />', () => {
  const SutComponent = (props) => (
    <CurrentUserContext.Provider value={{ ...currentUser, permissions: ['*'] }}>
      <UserEdit {...props} />
    </CurrentUserContext.Provider>
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('profile section', () => {
    it('should display username', async () => {
      render(<SutComponent user={user} />);

      await waitFor(() => expect(screen.getByText(user.username)).toBeInTheDocument());
    });

    it('should use user details as initial values', async () => {
      render(<SutComponent user={user} />);

      const submitButton = await waitFor(() => screen.getByText('Update Profile'));

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, {
        full_name: user.fullName,
        email: user.email,
      }));
    });

    it('should allow full name and e-mail address change', async () => {
      render(<SutComponent user={user} />);

      const fullNameInput = await screen.findByLabelText('Full Name');
      const emailInput = screen.getByLabelText('E-Mail Address');
      const submitButton = screen.getByText('Update Profile');

      fireEvent.change(fullNameInput, { target: { value: 'New full name' } });
      fireEvent.change(emailInput, { target: { value: 'newemail@example.org' } });

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, {
        full_name: 'New full name',
        email: 'newemail@example.org',
      }));
    });
  });

  describe('settings section', () => {
    it('should use user details as initial values', async () => {
      render(<SutComponent user={user} />);

      const submitButton = await screen.findByText('Update Settings');

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, {
        session_timeout_ms: user.sessionTimeoutMs,
        timezone: user.timezone,
      }));
    });

    it('should allow session timeout name and timezone change', async () => {
      render(<SutComponent user={user} />);

      const timeoutAmountInput = await screen.findByPlaceholderText('Timeout amount');
      const timezoneSelect = screen.getByLabelText('Time Zone');
      const submitButton = screen.getByText('Update Settings');

      fireEvent.change(timeoutAmountInput, { target: { value: '40' } });
      await selectEvent.openMenu(timezoneSelect);
      await selectEvent.select(timezoneSelect, 'Vancouver');
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, {
        session_timeout_ms: 144000000,
        timezone: 'America/Vancouver',
      }));
    });
  });

  describe('password section', () => {
    it('should allow password change', async () => {
      render(<SutComponent user={user} />);

      const newPasswordInput = await screen.findByLabelText('New Password');
      const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
      const submitButton = screen.getByText('Change Password');

      fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledWith(user.id, {
        password: 'newpassword',
      }));
    });

    it('should require current password when current user is changing his password', async () => {
      const newCurrentUser = User.fromJSON(currentUser).toBuilder().readOnly(false).build();
      render(<SutComponent user={newCurrentUser} />);

      const passwordInput = await screen.findByLabelText('Old Password');
      const newPasswordInput = screen.getByLabelText('New Password');
      const newPasswordRepeatInput = screen.getByLabelText('Repeat Password');
      const submitButton = screen.getByText('Change Password');

      fireEvent.change(passwordInput, { target: { value: 'oldpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledWith(newCurrentUser.id, {
        old_password: 'oldpassword',
        password: 'newpassword',
      }));
    });

    // The following test will work when we use @testing-library/user-event instead of fireEvent
    // it('should display warning, if password repeat does not match password', async () => {
    //   const { screen.getByLabelText, screen.getByText } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);

    //   const newPasswordInput = getByLabelText('New Password');
    //   const newPasswordRepeatInput = getByLabelText('Repeat Password');

    //   fireEvent.change(newPasswordInput, { target: { value: 'thepassword' } });
    //   fireEvent.change(newPasswordRepeatInput, { target: { value: 'notthepassword' } });

    //   await waitFor(() => expect(screen.getByText('Passwords do not match')).not.toBeNull());
    // });
  });

  describe('roles section', () => {
    // it('should assigning a role', async () => {
    //   const { getByLabelText, getByRole } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);

    //   const assignRoleButton = getByRole('button', { name: 'Assign Role' });
    //   const rolesSelector = getByLabelText('Search for roles');
    //   await selectEvent.openMenu(rolesSelector);
    //   await selectEvent.select(rolesSelector, notAssignedRole.name);
    //   fireEvent.click(assignRoleButton);

    //   await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, { roles: [assignedRole.name, notAssignedRole.name] }));
    // });

    // it('should filter assigned roles', async () => {
    //   const { getByPlaceholderText, getByRole } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);
    //   const filterInput = getByPlaceholderText('Enter query to filter');
    //   const filterSubmitButton = getByRole('button', { name: 'Filter' });

    //   fireEvent.change(filterInput, { target: { value: 'name of an assigned role' } });
    //   fireEvent.click(filterSubmitButton);

    //   await waitFor(() => expect(AuthzRolesActions.loadRolesForUser).toHaveBeenCalledWith(user.id, 1, 10, 'name of an assigned role'));
    // });

    // it('should unassign a role', async () => {
    //   const { getByRole } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);

    //   const assignRoleButton = getByRole('button', { name: `Remove ${assignedRole.name}` });
    //   fireEvent.click(assignRoleButton);

    //   await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.id, { roles: [] }));
    // });
  });

  describe('teams section', () => {
    it('should display info if license is not present', async () => {
      render(<SutComponent user={user} paginatedUserShares={undefined} />);

      await waitFor(() => expect(screen.getByText(/Enterprise Feature/)).not.toBeNull());
    });
  });
});
