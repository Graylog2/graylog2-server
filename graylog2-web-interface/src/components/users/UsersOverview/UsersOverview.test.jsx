// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, cleanup, fireEvent } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import UsersActions from 'actions/users/UsersActions';
import UsersStore from 'stores/users/UsersStore';
import { alice, bob, admin } from 'stores/users/users';
import CurrentUserContext from 'contexts/CurrentUserContext';

import UsersOverview from './UsersOverview';

const mockUsers = Immutable.List([alice, bob, admin]);

jest.mock('stores/users/UsersStore', () => ({
  listen: jest.fn(),
  getInitialState: jest.fn(() => ({ list: mockUsers })),
}));

jest.mock('actions/users/UsersActions', () => ({
  loadUsers: jest.fn(),
  deleteUser: jest.fn(),
}));

describe('UsersOverview', () => {
  beforeEach(() => {
    UsersActions.deleteUser.completed = { listen: jest.fn(() => jest.fn()) };
  });

  afterEach(() => {
    cleanup();
  });

  describe('should display table header', () => {
    const displaysHeader = ({ header }) => {
      const { queryByText } = render(<UsersOverview />);

      expect(queryByText(header)).not.toBeNull();
    };

    it.each`
      header
      ${'Username'}
      ${'Full name'}
      ${'E-Mail Address'}
      ${'Client Address'}
      ${'Role'}
      ${'Actions'}
    `('$header', displaysHeader);
  });

  describe('should display user', () => {
    const displaysUserAttributes = ({ user }) => {
      const { queryByText } = render(<UsersOverview />);
      const attributes = ['username', 'fullName', 'email', 'clientAddress'];

      attributes.forEach((attribute) => {
        if (user[attribute]) {
          expect(queryByText(user[attribute])).not.toBeNull();
        }
      });
    };

    it.each`
      user     | username
      ${alice} | ${alice.username}
      ${bob}   | ${bob.username}
      ${admin} | ${admin.username}
    `('$username', displaysUserAttributes);
  });

  describe('admin should', () => {
    const modifiableUser = alice.toBuilder().readOnly(false).build();
    const modifiableUsersList = Immutable.List([modifiableUser]);
    const readOnlyUser = alice.toBuilder().readOnly(true).build();
    const readOnlyUsersList = Immutable.List([readOnlyUser]);
    let oldConfirm;

    const UsersOverviewAsAdmin = () => (
      <CurrentUserContext.Provider value={admin}>
        <UsersOverview />
      </CurrentUserContext.Provider>
    );

    beforeEach(() => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => true);
    });

    afterEach(() => {
      window.confirm = oldConfirm;
    });

    it('be able to delete a modifiable user', () => {
      asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: modifiableUsersList });
      const { getByTitle } = render(<UsersOverviewAsAdmin />);

      fireEvent.click(getByTitle(`Delete user ${modifiableUser.username}`));

      expect(window.confirm).toHaveBeenCalledTimes(1);
      expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete user ${modifiableUser.username}?`);
      expect(UsersActions.deleteUser).toHaveBeenCalledTimes(1);
      expect(UsersActions.deleteUser).toHaveBeenCalledWith(alice.username);
    });

    it('not be able to delete a "read only" user', () => {
      asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      expect(queryByTitle(`Delete user ${readOnlyUser.username}`)).toBeNull();
    });

    it('see edit and edit tokens link for a modifiable user ', () => {
      asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: modifiableUsersList });
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      expect(queryByTitle(`Edit user ${modifiableUser.username}`)).not.toBeNull();
      expect(queryByTitle(`Edit tokens of user ${modifiableUser.username}`)).not.toBeNull();
    });

    it('not see edit link for a "read only" user', () => {
      asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      expect(queryByTitle(`Edit user ${readOnlyUser.username}`)).toBeNull();
    });

    it('see edit tokens link for a "read only" user', () => {
      asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      expect(queryByTitle(`Edit tokens for user ${readOnlyUser.username}`)).toBeNull();
    });
  });
});
