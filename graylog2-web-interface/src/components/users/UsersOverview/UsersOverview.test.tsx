/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';

import { paginatedUsers, alice, bob, admin as adminOverview } from 'fixtures/userOverviews';
import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';
import { UsersActions } from 'stores/users/UsersStore';

import UsersOverview from './UsersOverview';

// The usage of OverlayTrigger in the StatusCell of the users overview
// often results in a timeout when executing the 'should search users' test.
// We need to mock OverlayTrigger until we fix the root problem.
jest.mock('components/common/OverlayTrigger', () => 'overlay-trigger');

const mockLoadUsersPaginatedPromise = Promise.resolve(paginatedUsers);

jest.mock('stores/users/UsersStore', () => ({
  UsersActions: {
    loadUsersPaginated: jest.fn(() => mockLoadUsersPaginatedPromise),
    delete: mockAction(),
    setStatus: mockAction(),
  },
}));

describe('UsersOverview', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display table header', async () => {
    render(<UsersOverview />);
    const headers = [
      'Username',
      'Full name',
      'E-Mail Address',
      'Client Address',
      'Role',
      'Actions',
    ];

    // wait until list is displayed
    await screen.findByText('Users');

    headers.forEach((header) => {
      expect(screen.getByText(header)).toBeInTheDocument();
    });
  });

  it('should search users', async () => {
    render(<UsersOverview />);
    const searchInput = await screen.findByPlaceholderText('Enter search query...');

    fireEvent.change(searchInput, { target: { value: 'username:bob' } });

    await waitFor(() => expect(UsersActions.loadUsersPaginated).toHaveBeenCalledWith({ page: 1, perPage: 10, query: 'username:bob' }));
  });

  describe('should display user', () => {
    it.each`
      user     | username
      ${alice} | ${alice.username}
      ${bob}   | ${bob.username}
      ${adminOverview} | ${adminOverview.username}
    `('$username', async ({ user }) => {
      render(<UsersOverview />);
      const attributes = ['username', 'fullName', 'email', 'clientAddress'];
      // wait until list is displayed
      await screen.findByText('Users');

      attributes.forEach(async (attribute) => {
        if (user[attribute]) {
          // eslint-disable-next-line jest/no-conditional-expect
          expect(screen.getByText(user[attribute])).toBeInTheDocument();
        }
      });
    });
  });

  describe('admin should', () => {
    const modifiableUser = alice.toBuilder().readOnly(false).build();
    const modifiableUsersList = Immutable.List([modifiableUser]);
    const readOnlyUser = alice.toBuilder().readOnly(true).build();
    const readOnlyUsersList = Immutable.List([readOnlyUser]);
    let oldConfirm;

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
      render(<UsersOverview />);

      const deleteButton = await screen.findByTitle(`Delete user ${modifiableUser.fullName}`);
      fireEvent.click(deleteButton);

      expect(window.confirm).toHaveBeenCalledTimes(1);
      expect(window.confirm).toHaveBeenCalledWith(`Do you really want to delete user ${modifiableUser.fullName}?`);
      expect(UsersActions.delete).toHaveBeenCalledTimes(1);
      expect(UsersActions.delete).toHaveBeenCalledWith(modifiableUser.id, modifiableUser.fullName);
    });

    it('not be able to delete a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      render(<UsersOverview />);

      await waitFor(() => expect(screen.queryByTitle(`Delete user ${readOnlyUser.fullName}`)).not.toBeInTheDocument());
    });

    it('see edit and edit tokens link for a modifiable user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: modifiableUsersList }));
      render(<UsersOverview />);

      await screen.findByTitle(`Edit user ${modifiableUser.fullName}`);

      expect(screen.getByTitle(`Edit tokens of user ${modifiableUser.fullName}`)).toBeInTheDocument();
    });

    it('not see edit link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      render(<UsersOverview />);

      await waitFor(() => expect(screen.queryByTitle(`Edit user ${readOnlyUser.fullName}`)).not.toBeInTheDocument());
    });

    it('see edit tokens link for a "read only" user', async () => {
      asMock(UsersActions.loadUsersPaginated).mockReturnValueOnce(Promise.resolve({ ...paginatedUsers, list: readOnlyUsersList }));
      render(<UsersOverview />);

      await screen.findByTitle(`Edit tokens of user ${readOnlyUser.fullName}`);
    });
  });
});
