// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, waitFor, fireEvent } from 'wrappedTestingLibrary';
import { admin } from 'fixtures/users';
import { paginatedUsers, alice, bob, admin as adminOverview } from 'fixtures/userOverviews';
import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { UsersActions } from 'stores/users/UsersStore';

import UsersOverview from './UsersOverview';

const mockLoadUsersPaginatedPromise = Promise.resolve(paginatedUsers);

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPaginatedPromise),
    delete: mockAction(jest.fn(() => Promise.resolve())),
  },
}));

describe('UsersOverview', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display table header', async () => {
    const { findByPlaceholderText, getByText } = render(<UsersOverview />);
    const headers = [
      'Username',
      'Full name',
      'E-Mail Address',
      'Client Address',
      'Role',
      'Actions',
    ];

    await findByPlaceholderText('Enter search query...');

    headers.forEach((header) => {
      expect(getByText(header)).toBeInTheDocument();
    });
  });

  it('should search users', async () => {
    const { findByPlaceholderText, getByRole } = render(<UsersOverview />);
    const searchInput = await findByPlaceholderText('Enter search query...');
    const searchSubmitButton = getByRole('button', { name: 'Search' });

    fireEvent.change(searchInput, { target: { value: 'username:bob' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(UsersActions.loadUsersPaginated).toHaveBeenCalledWith({ page: 1, perPage: 10, query: 'username:bob' }));
  });

  describe('should display user', () => {
    const displaysUserAttributes = async ({ user }) => {
      const { getByText, findByPlaceholderText } = render(<UsersOverview />);
      const attributes = ['username', 'fullName', 'email', 'clientAddress'];
      await findByPlaceholderText('Enter search query...');

      attributes.forEach(async (attribute) => {
        if (user[attribute]) {
          expect(getByText(user[attribute])).toBeInTheDocument();
        }
      });
    };

    it.each`
      user     | username
      ${alice} | ${alice.username}
      ${bob}   | ${bob.username}
      ${adminOverview} | ${adminOverview.username}
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

    it('be able to delete a modifiable user', async () => {
      const loadUsersPaginatedPromise = Promise.resolve({ ...paginatedUsers, list: modifiableUsersList });
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(loadUsersPaginatedPromise);
      const { findByTitle } = render(<UsersOverviewAsAdmin />);

      const deleteButton = await findByTitle(`Delete user ${modifiableUser.username}`);
      fireEvent.click(deleteButton);

      expect(window.confirm).toHaveBeenCalledTimes(1);
      expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete user ${modifiableUser.username}?`);
      expect(UsersActions.delete).toHaveBeenCalledTimes(1);
      expect(UsersActions.delete).toHaveBeenCalledWith(alice.username);
    });

    it('not be able to delete a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { findByTitle } = render(<UsersOverviewAsAdmin />);

      await findByTitle(`Delete user ${readOnlyUser.username}`);
    });

    it('see edit and edit tokens link for a modifiable user ', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: modifiableUsersList }));
      const { findByTitle } = render(<UsersOverviewAsAdmin />);

      await findByTitle(`Edit user ${modifiableUser.username}`);
      // await findByTitle(`Edit tokens of user ${modifiableUser.username}`);
    });

    it('not see edit link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { findByTitle } = render(<UsersOverviewAsAdmin />);

      await findByTitle(`Edit user ${readOnlyUser.username}`);
    });

    it('see edit tokens link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { findByTitle } = render(<UsersOverviewAsAdmin />);

      await findByTitle(`Edit tokens of user ${readOnlyUser.username}`);
    });
  });
});
