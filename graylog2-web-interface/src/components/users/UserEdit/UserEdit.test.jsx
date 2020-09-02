// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import { render, fireEvent, waitFor, act } from 'wrappedTestingLibrary';
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

const mockRolesForUserPromise = Promise.resolve({ list: Immutable.List([assignedRole]), pagination: { total: 1, page: 1, perPage: 10 } });
const mockLoadRolesPromise = Promise.resolve({ list: Immutable.List([notAssignedRole]), pagination: { total: 1, page: 1, perPage: 10 } });
const user = User
  .builder()
  .fullName('The full name')
  .username('The username')
  .roles(Immutable.List([assignedRole.name]))
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
      const { getByText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      expect(getByText(user.username)).not.toBeNull();
    });

    it('should use user details as initial values', async () => {
      const { getByText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);
      const submitButton = getByText('Update Profile');

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        full_name: user.fullName,
        email: user.email,
      }));
    });

    it('should allow full name and e-mail address change', async () => {
      const { getByText, getByLabelText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const fullNameInput = getByLabelText('Full Name');
      const emailInput = getByLabelText('E-Mail Address');
      const submitButton = getByText('Update Profile');

      fireEvent.change(fullNameInput, { target: { value: 'New full name' } });
      fireEvent.change(emailInput, { target: { value: 'newemail@example.org' } });

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        full_name: 'New full name',
        email: 'newemail@example.org',
      }));
    });
  });

  describe('settings section', () => {
    it('should use user details as initial values', async () => {
      const { getByText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const submitButton = getByText('Update Settings');

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        session_timeout_ms: user.sessionTimeoutMs,
        timezone: user.timezone,
      }));
    });

    it('should allow session timeout name and timezone change', async () => {
      const { getByText, getByLabelText, getByPlaceholderText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const timeoutAmountInput = getByPlaceholderText('Timeout amount');
      // const timeoutUnitSelect = getByLabelText('Timeout unit');
      const timezoneSelect = getByLabelText('Time Zone');
      const submitButton = getByText('Update Settings');

      fireEvent.change(timeoutAmountInput, { target: { value: '40' } });
      // await act(async () => { await selectEvent.openMenu(timeoutUnitSelect); });
      // await act(async () => { await selectEvent.select(timeoutUnitSelect, 'Hours'); });
      await act(async () => { await selectEvent.openMenu(timezoneSelect); });
      await act(async () => { await selectEvent.select(timezoneSelect, 'Vancouver'); });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        session_timeout_ms: 144000000,
        timezone: 'America/Vancouver',
      }));
    });
  });

  describe('password section', () => {
    it('should allow password change', async () => {
      const { getByLabelText, getByText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const newPasswordInput = getByLabelText('New Password');
      const newPasswordRepeatInput = getByLabelText('Repeat Password');
      const submitButton = getByText('Change Password');

      fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledWith(user.username, {
        password: 'newpassword',
      }));
    });

    it('should require current password when current user is changing his password', async () => {
      const { getByLabelText, getByText } = render(<SutComponent user={User.fromJSON(currentUser)} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const passwordInput = getByLabelText('Old Password');
      const newPasswordInput = getByLabelText('New Password');
      const newPasswordRepeatInput = getByLabelText('Repeat Password');
      const submitButton = getByText('Change Password');

      fireEvent.change(passwordInput, { target: { value: 'oldpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledWith(currentUser.username, {
        old_password: 'oldpassword',
        password: 'newpassword',
      }));
    });

    it('should display warning, if password repeat does not match password', async () => {
      const { getByLabelText, getByText } = render(<SutComponent user={user} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      const newPasswordInput = getByLabelText('New Password');
      const newPasswordRepeatInput = getByLabelText('Repeat Password');

      fireEvent.change(newPasswordInput, { target: { value: 'thepassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'notthepassword' } });

      await waitFor(() => expect(getByText('Passwords do not match')).not.toBeNull());
    });
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

    //   await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, { roles: [assignedRole.name, notAssignedRole.name] }));
    // });

    // it('should filter assigned roles', async () => {
    //   const { getByPlaceholderText, getByRole } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);
    //   const filterInput = getByPlaceholderText('Enter query to filter');
    //   const filterSubmitButton = getByRole('button', { name: 'Filter' });

    //   fireEvent.change(filterInput, { target: { value: 'name of an assigned role' } });
    //   fireEvent.click(filterSubmitButton);

    //   await waitFor(() => expect(AuthzRolesActions.loadRolesForUser).toHaveBeenCalledWith(user.username, 1, 10, 'name of an assigned role'));
    // });

    // it('should unassigning a role', async () => {
    //   const { getByRole } = render(<SutComponent user={user} />);
    //   await act(() => mockRolesForUserPromise);
    //   await act(() => mockLoadRolesPromise);

    //   const assignRoleButton = getByRole('button', { name: `Remove ${assignedRole.name}` });
    //   fireEvent.click(assignRoleButton);

    //   await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, { roles: [] }));
    // });
  });

  describe('teams section', () => {
    it('should display info if license is not present', async () => {
      const { getByText } = render(<SutComponent user={user} paginatedUserShares={undefined} />);
      await act(() => mockRolesForUserPromise);
      await act(() => mockLoadRolesPromise);

      expect(getByText('No enterprise plugin found')).not.toBeNull();
    });
  });
});
