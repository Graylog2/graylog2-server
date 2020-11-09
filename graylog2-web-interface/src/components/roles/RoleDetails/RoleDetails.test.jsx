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
