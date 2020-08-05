// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, waitFor } from 'wrappedTestingLibrary';
import { alice, bob, adminUser } from 'fixtures/users';

import { UsersActions } from 'stores/users/UsersStore';

import UsersOverview from './UsersOverview';

const mockUsers = Immutable.List([alice, bob, adminUser]);
const mockPaginatedUsersPromise = Promise.resolve({
  list: mockUsers,
  pagination: {
    page: 1,
    perPage: 10,
    total: mockUsers.size,
  },
  adminUser: undefined,
});

jest.mock('stores/users/UsersStore', () => ({
  UsersStore: {
    listen: jest.fn(),
  },
  UsersActions: {
    searchPaginated: jest.fn(() => mockPaginatedUsersPromise),
    deleteUser: jest.fn(),
  },
}));

describe('UsersOverview', () => {
  beforeEach(() => {
    UsersActions.deleteUser.completed = { listen: jest.fn(() => jest.fn()) };
  });

  describe('should display table header', () => {
    const displaysHeader = async ({ header }) => {
      const { queryByText } = render(<UsersOverview />);

      await waitFor(() => expect(queryByText(header)).not.toBeNull());
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
    const displaysUserAttributes = async ({ user }) => {
      const { queryByText } = render(<UsersOverview />);
      const attributes = ['username', 'fullName', 'email', 'clientAddress'];

      await act(() => mockPaginatedUsersPromise);

      attributes.forEach(async (attribute) => {
        if (user[attribute]) {
          expect(queryByText(user[attribute])).not.toBeNull();
        }
      });
    };

    it.each`
      user     | username
      ${alice} | ${alice.username}
      ${bob}   | ${bob.username}
      ${adminUser} | ${adminUser.username}
    `('$username', displaysUserAttributes);
  });

  // describe('admin should', () => {
  //   const modifiableUser = alice.toBuilder().readOnly(false).build();
  //   const modifiableUsersList = Immutable.List([modifiableUser]);
  //   const readOnlyUser = alice.toBuilder().readOnly(true).build();
  //   const readOnlyUsersList = Immutable.List([readOnlyUser]);
  //   let oldConfirm;

  //   const UsersOverviewAsAdmin = () => (
  //     <CurrentUserContext.Provider value={admin}>
  //       <UsersOverview />
  //     </CurrentUserContext.Provider>
  //   );

  //   beforeEach(() => {
  //     oldConfirm = window.confirm;
  //     window.confirm = jest.fn(() => true);
  //   });

  //   afterEach(() => {
  //     window.confirm = oldConfirm;
  //   });

  //   it('be able to delete a modifiable user', () => {
  //     asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: modifiableUsersList });
  //     const { getByTitle } = render(<UsersOverviewAsAdmin />);

  //     fireEvent.click(getByTitle(`Delete user ${modifiableUser.username}`));

  //     expect(window.confirm).toHaveBeenCalledTimes(1);
  //     expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete user ${modifiableUser.username}?`);
  //     expect(UsersActions.deleteUser).toHaveBeenCalledTimes(1);
  //     expect(UsersActions.deleteUser).toHaveBeenCalledWith(alice.username);
  //   });

  //   it('not be able to delete a "read only" user', () => {
  //     asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
  //     const { queryByTitle } = render(<UsersOverviewAsAdmin />);

  //     expect(queryByTitle(`Delete user ${readOnlyUser.username}`)).toBeNull();
  //   });

  //   it('see edit and edit tokens link for a modifiable user ', () => {
  //     asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: modifiableUsersList });
  //     const { queryByTitle } = render(<UsersOverviewAsAdmin />);

  //     expect(queryByTitle(`Edit user ${modifiableUser.username}`)).not.toBeNull();
  //     expect(queryByTitle(`Edit tokens of user ${modifiableUser.username}`)).not.toBeNull();
  //   });

  //   it('not see edit link for a "read only" user', () => {
  //     asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
  //     const { queryByTitle } = render(<UsersOverviewAsAdmin />);

  //     expect(queryByTitle(`Edit user ${readOnlyUser.username}`)).toBeNull();
  //   });

  //   it('see edit tokens link for a "read only" user', () => {
  //     asMock(UsersStore.getInitialState).mockReturnValueOnce({ list: readOnlyUsersList });
  //     const { queryByTitle } = render(<UsersOverviewAsAdmin />);

  //     expect(queryByTitle(`Edit tokens for user ${readOnlyUser.username}`)).toBeNull();
  //   });
  // });
});
