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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { paginatedShares } from 'fixtures/sharedEntities';
import { reader as assignedRole } from 'fixtures/roles';
import { alice } from 'fixtures/users';
import CurrentUserContext from 'contexts/CurrentUserContext';
import User from 'logic/users/User';

import UserDetails from './UserDetails';

const mockAuthzRolesPromise = Promise.resolve({ list: Immutable.List([assignedRole]), pagination: { page: 1, perPage: 10, total: 1 } });
const mockPaginatedUserShares = paginatedShares({ page: 1, perPage: 10, query: '' });

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesForUser: jest.fn(() => mockAuthzRolesPromise),
    loadRolesPaginated: jest.fn(() => mockAuthzRolesPromise),
  },
}));

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    loadUserSharesPaginated: jest.fn(() => Promise.resolve(mockPaginatedUserShares)),
  },
}));

const user = User
  .builder()
  .fullName('The full name')
  .firstName('The first name')
  .lastName('The last name')
  .username('The username')
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

describe('<UserDetails />', () => {
  const currentUser = alice.toBuilder().permissions(Immutable.List(['*'])).build();
  const SutComponent = (props) => (
    <CurrentUserContext.Provider value={currentUser}>
      <UserDetails {...props} />
    </CurrentUserContext.Provider>
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('user profile should display profile information', async () => {
    render(<SutComponent user={user} />);

    await screen.findByText(user.username);

    expect(screen.getByText(user.firstName)).toBeInTheDocument();
    expect(screen.getByText(user.lastName)).toBeInTheDocument();
    expect(screen.getByText(user.email)).toBeInTheDocument();
    expect(screen.getByText(user.clientAddress)).toBeInTheDocument();

    if (!user.lastActivity) throw Error('lastActivity must be defined for provided user');

    expect(screen.getByText(user.lastActivity)).toBeInTheDocument();
  });

  describe('user settings', () => {
    it('should display timezone', async () => {
      render(<SutComponent user={user} />);

      await waitFor(() => {
        if (!user.timezone) throw Error('timezone must be defined for provided user');

        return expect(screen.getByText(user.timezone)).toBeInTheDocument();
      });
    });

    describe('should display session timeout in a readable format', () => {
      it('for seconds', async () => {
        const test = user.toBuilder().sessionTimeoutMs(10000).build();
        render(<SutComponent user={test} />);

        await screen.findByText('10 Seconds');
      });

      it('for minutes', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(600000).build()} />);

        await screen.findByText('10 Minutes');
      });

      it('for hours', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(36000000).build()} />);

        await screen.findByText('10 Hours');
      });

      it('for days', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(864000000).build()} />);

        await screen.findByText('10 Days');
      });
    });
  });

  describe('roles section', () => {
    it('should display assigned roles', async () => {
      render(<SutComponent user={user} />);

      await screen.findByText(assignedRole.name);
    });
  });

  describe('teams section', () => {
    it('should display info if license is not present', async () => {
      render(<SutComponent user={user} />);

      await screen.findByText(/Enterprise Feature/);
    });
  });
});
