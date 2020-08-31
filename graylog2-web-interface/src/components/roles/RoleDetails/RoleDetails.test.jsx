// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act } from 'wrappedTestingLibrary';
import { alertsManager } from 'fixtures/roles';
import { alice } from 'fixtures/userOverviews';

import RoleDetails from './RoleDetails';

const paginatedUsers = {
  list: Immutable.List([alice]),
  pagination: {
    page: 1,
    perPage: 10,
    total: 1,
  },
};

const mockLoadUsersPromise = Promise.resolve(paginatedUsers);

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesStore: {},
  AuthzRolesActions: {
    loadUsersForRole: jest.fn(() => mockLoadUsersPromise),
  },
}));

jest.useFakeTimers();

describe('RoleDetails', () => {
  it('should display loading indicator, if no role is provided', async () => {
    const { queryByText } = render(<RoleDetails role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(queryByText('Loading...')).not.toBeNull();
  });

  it('should display role profile', async () => {
    const { queryByText } = render(<RoleDetails role={alertsManager} />);
    await act(() => mockLoadUsersPromise);

    expect(queryByText(alertsManager.name)).not.toBeNull();
    expect(queryByText(alertsManager.description)).not.toBeNull();
  });

  it('should display assigned users', async () => {
    const { queryByText, queryByRole } = render(<RoleDetails role={alertsManager} />);
    await act(() => mockLoadUsersPromise);

    expect(queryByRole('heading', { level: 2, name: 'Users' })).not.toBeNull();
    expect(queryByText(alice.fullName)).not.toBeNull();
  });
});
