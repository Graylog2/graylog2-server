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
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, screen } from 'wrappedTestingLibrary';
import { alertsManager as exampleRole } from 'fixtures/roles';
import { alice as assignedUser } from 'fixtures/userOverviews';

import RoleDetails from './RoleDetails';

const mockLoadUsersPromise = Promise.resolve({
  list: Immutable.List([assignedUser]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
});

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesStore: {},
  AuthzRolesActions: {
    loadUsersForRole: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.useFakeTimers();

describe('RoleDetails', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display loading indicator, if no role is provided', async () => {
    render(<RoleDetails role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should display role profile', async () => {
    render(<RoleDetails role={exampleRole} />);

    await screen.findByText(exampleRole.name);
    await screen.findByText(exampleRole.description);
  });

  it('should display assigned users', async () => {
    render(<RoleDetails role={exampleRole} />);

    await screen.findByRole('heading', { level: 2, name: 'Users' });
    await screen.findByText(assignedUser.fullName);
  });
});
