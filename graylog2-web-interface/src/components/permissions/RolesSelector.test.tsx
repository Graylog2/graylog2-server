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

import { rolesList as mockRoles } from 'fixtures/roles';

import RolesSelector from './RolesSelector';

const mockLoadRolesPaginatedResponse = {
  list: mockRoles,
  pagination: {
    page: 1,
    perPage: 10,
    query: '',
    count: mockRoles.size,
    total: mockRoles.size,
  },
};

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadRolesPaginated: jest.fn(() => Promise.resolve(mockLoadRolesPaginatedResponse)),
  },
}));

describe('RolesSelector', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders the roles select and button', async () => {
    render(<RolesSelector assignedRolesIds={Immutable.Set([''])} identifier={(role) => role.name} onSubmit={jest.fn()} />);

    await screen.findByText(/search for roles/i);
    await screen.findByRole('button', { name: /assign role/i });
  });

  it('does not render the button when submitOnSelect=true', async () => {
    render(<RolesSelector assignedRolesIds={Immutable.Set([''])} identifier={(role) => role.name} onSubmit={jest.fn()} submitOnSelect />);

    const submitButton = screen.queryByRole('button', { name: /assign role/i });

    await waitFor(() => {
      expect(submitButton).not.toBeInTheDocument();
    });
  });
});
