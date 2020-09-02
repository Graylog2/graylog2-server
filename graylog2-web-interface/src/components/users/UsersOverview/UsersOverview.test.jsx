// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, waitFor, fireEvent } from 'wrappedTestingLibrary';
import { admin } from 'fixtures/users';
import { paginatedUsers, alice, bob, admin as adminOverview } from 'fixtures/userOverviews';
import asMock from 'helpers/mocking/AsMock';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { UsersActions } from 'stores/users/UsersStore';

import UsersOverview from './UsersOverview';

const mockLoadUsersPaginatedPromise = Promise.resolve(paginatedUsers);

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPaginatedPromise),
    delete: jest.fn(() => Promise.resolve()),
  },
}));

describe('UsersOverview', () => {
  beforeEach(() => {
    UsersActions.delete.completed = { listen: jest.fn(() => jest.fn()) };
  });

  afterEach(() => {
    jest.clearAllMocks();
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
      await act(() => mockLoadUsersPaginatedPromise);

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
      ${adminOverview} | ${adminOverview.username}
    `('$username', displaysUserAttributes);
  });

  it('should search users', async () => {
    const { getByPlaceholderText, getByRole } = render(<UsersOverview />);
    await act(() => mockLoadUsersPaginatedPromise);
    const searchInput = getByPlaceholderText('Enter search query...');
    const searchSubmitButton = getByRole('button', { name: 'Search' });

    fireEvent.change(searchInput, { target: { value: 'username:bob' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(UsersActions.loadUsersPaginated).toHaveBeenCalledWith(1, 10, 'username:bob'));
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
      const { getByTitle } = render(<UsersOverviewAsAdmin />);
      await act(() => loadUsersPaginatedPromise);

      fireEvent.click(getByTitle(`Delete user ${modifiableUser.username}`));

      expect(window.confirm).toHaveBeenCalledTimes(1);
      expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete user ${modifiableUser.username}?`);
      expect(UsersActions.delete).toHaveBeenCalledTimes(1);
      expect(UsersActions.delete).toHaveBeenCalledWith(alice.username);
    });

    it('not be able to delete a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      await waitFor(() => expect(queryByTitle(`Delete user ${readOnlyUser.username}`)).toBeNull());
    });

    it('see edit and edit tokens link for a modifiable user ', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: modifiableUsersList }));
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      await waitFor(() => {
        expect(queryByTitle(`Edit user ${modifiableUser.username}`)).not.toBeNull();
        expect(queryByTitle(`Edit tokens of user ${modifiableUser.username}`)).not.toBeNull();
      });
    });

    it('not see edit link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      await waitFor(() => expect(queryByTitle(`Edit user ${readOnlyUser.username}`)).toBeNull());
    });

    it('see edit tokens link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      const { queryByTitle } = render(<UsersOverviewAsAdmin />);

      await waitFor(() => expect(queryByTitle(`Edit tokens for user ${readOnlyUser.username}`)).toBeNull());
    });
  });
});
