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
import userEvent from '@testing-library/user-event';

import { reader as assignedRole } from 'fixtures/roles';
import User from 'logic/users/User';

import UserDetails from './UserDetails';

const mockAuthzRolesPromise = Promise.resolve({
  list: Immutable.List([assignedRole]),
  pagination: { page: 1, perPage: 10, total: 1 },
});

jest.mock('hooks/useAuthzRoles', () => ({
  AUTHZ_ROLES_QUERY_KEY: ['authz', 'roles'],
  loadRolesForUser: jest.fn(() => mockAuthzRolesPromise),
  loadRolesPaginated: jest.fn(() => mockAuthzRolesPromise),
}));

jest.mock('api/entity-share', () => ({
  prepareEntityShare: jest.fn(() => Promise.resolve()),
  updateEntityShare: jest.fn(() => Promise.resolve()),
  loadUserSharesPaginated: jest.fn(() =>
    Promise.resolve({
      list: require('immutable').List(),
      pagination: { page: 1, perPage: 10, query: '', total: 0, count: 0 },
    }),
  ),
}));
jest.mock('hooks/useEntityShareState', () => {
  const mockSetEntityShareState = jest.fn();

  return {
    __esModule: true,
    default: jest.fn(() => ({ data: undefined })),
    useSetEntityShareState: jest.fn(() => mockSetEntityShareState),
    entityShareQueryKey: jest.fn((grn) => ['entity-share', grn ?? 'new']),
  };
});

const user = User.builder()
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

describe('UserDetails', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('user profile should display profile information', async () => {
    render(<UserDetails user={user} />);

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
      render(<UserDetails user={user} />);
      const tab = await screen.findByLabelText(/Preferences/i);
      await userEvent.click(tab);
      await waitFor(() => {
        if (!user.timezone) throw Error('timezone must be defined for provided user');

        return expect(screen.getByText(user.timezone)).toBeInTheDocument();
      });
    });

    describe('should display session timeout in a readable format', () => {
      it('for seconds', async () => {
        const exampleUser = user.toBuilder().sessionTimeoutMs(10000).build();
        render(<UserDetails user={exampleUser} />);

        const tab = await screen.findByLabelText(/Preferences/i);
        await userEvent.click(tab);

        await screen.findByText('10 Seconds');
      });

      it('for minutes', async () => {
        render(<UserDetails user={user.toBuilder().sessionTimeoutMs(600000).build()} />);

        const tab = await screen.findByLabelText(/Preferences/i);
        await userEvent.click(tab);

        await screen.findByText('10 Minutes');
      });

      it('for hours', async () => {
        render(<UserDetails user={user.toBuilder().sessionTimeoutMs(36000000).build()} />);

        const tab = await screen.findByLabelText(/Preferences/i);
        await userEvent.click(tab);

        await screen.findByText('10 Hours');
      });

      it('for days', async () => {
        render(<UserDetails user={user.toBuilder().sessionTimeoutMs(864000000).build()} />);

        const tab = await screen.findByLabelText(/Preferences/i);
        await userEvent.click(tab);

        await screen.findByText('10 Days');
      });
    });
  });

  describe('roles section', () => {
    it('should display assigned roles', async () => {
      render(<UserDetails user={user} />);

      const tab = await screen.findByLabelText(/Teams & Roles/i);
      await userEvent.click(tab);

      await screen.findByText(assignedRole.name);
    });
  });

  describe('teams section', () => {
    it('should display info if license is not present', async () => {
      render(<UserDetails user={user} />);

      const tab = await screen.findByLabelText(/Teams & Roles/i);
      await userEvent.click(tab);

      await screen.findAllByText(/Enterprise Feature/);
    });
  });
});
