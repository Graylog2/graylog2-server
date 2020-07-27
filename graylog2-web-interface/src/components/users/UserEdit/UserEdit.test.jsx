// @flow strict
import React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { act } from 'react-dom/test-utils';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { UsersActions } from 'stores/users/UsersStore';
import User from 'logic/users/User';

import UserEdit from './UserEdit';

const user = User
  .builder()
  .fullName('The full name')
  .username('The username')
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    update: jest.fn(() => Promise.resolve()),
    changePassword: jest.fn(() => Promise.resolve()),
  },
}));

describe('<UserEdit />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('profile section', () => {
    it('should display username', () => {
      const { getByText } = render(<UserEdit user={user} />);

      expect(getByText(user.username)).not.toBeNull();
    });

    it('should use user details as initial values', async () => {
      const { getByText } = render(<UserEdit user={user} />);

      const submitButton = getByText('Update Profile');

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        full_name: user.fullName,
        email: user.email,
      }));
    });

    it('should allow full name and e-mail address change', async () => {
      const { getByText, getByLabelText } = render(<UserEdit user={user} />);

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
      const { getByText } = render(<UserEdit user={user} />);

      const submitButton = getByText('Update Settings');

      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.update).toHaveBeenCalledWith(user.username, {
        session_timeout_ms: user.sessionTimeoutMs,
        timezone: user.timezone,
      }));
    });

    it('should allow session timeout name and timezone change', async () => {
      const { getByText, getByLabelText, getByPlaceholderText, getByTestId } = render(<UserEdit user={user} />);

      const timeoutAmountInput = getByPlaceholderText('Timeout amount');
      const timeoutUnitSelect = getByTestId('timeout-unit-select');
      const timezoneSelect = getByLabelText('Time Zone');
      const submitButton = getByText('Update Settings');

      fireEvent.change(timeoutAmountInput, { target: { value: '40' } });
      await selectEvent.openMenu(timeoutUnitSelect);
      await act(async () => { await selectEvent.select(timeoutUnitSelect, 'Hours'); });
      await selectEvent.openMenu(timezoneSelect);
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
      const { getByLabelText, getByText } = render(<UserEdit user={user} />);

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
      const { getByLabelText, getByText } = render(
        <CurrentUserContext.Provider value={{ ...user.toJSON(), permissions: ['users:passwordchange:*'] }}>
          <UserEdit user={user} />
        </CurrentUserContext.Provider>,
      );

      const oldPsswordInput = getByLabelText('Old Password');
      const newPasswordInput = getByLabelText('New Password');
      const newPasswordRepeatInput = getByLabelText('Repeat Password');
      const submitButton = getByText('Change Password');

      fireEvent.change(oldPsswordInput, { target: { value: 'oldpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'newpassword' } });
      fireEvent.click(submitButton);

      await waitFor(() => expect(UsersActions.changePassword).toHaveBeenCalledWith(user.username, {
        old_password: 'oldpassword',
        password: 'newpassword',
      }));
    });

    it('should display warning, if password repeat does not match password', async () => {
      const { getByLabelText, getByText } = render(<UserEdit user={user} />);

      const newPasswordInput = getByLabelText('New Password');
      const newPasswordRepeatInput = getByLabelText('Repeat Password');

      fireEvent.change(newPasswordInput, { target: { value: 'thepassword' } });
      fireEvent.change(newPasswordRepeatInput, { target: { value: 'notthepassword' } });

      await waitFor(() => expect(getByText('Passwords do not match')).not.toBeNull());
    });
  });
});
