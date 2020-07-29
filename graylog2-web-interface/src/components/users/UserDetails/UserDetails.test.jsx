// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act } from 'wrappedTestingLibrary';

import User from 'logic/users/User';

import UserDetails from './UserDetails';

const mockAuthzRolesPromise = Promise.resolve({ list: Immutable.List(), pagination: { total: 0 } });

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadForUser: jest.fn(() => mockAuthzRolesPromise),
    loadPaginated: jest.fn(() => mockAuthzRolesPromise),
  },
}));

const user = User
  .builder()
  .fullName('The full name')
  .username('The username')
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

describe('<UserDetails />', () => {
  it('user profile should display profile information', async () => {
    const { getByText } = render(<UserDetails user={user} paginatedUserShares={undefined} />);

    expect(getByText(user.username)).not.toBeNull();
    expect(getByText(user.fullName)).not.toBeNull();
    expect(getByText(user.email)).not.toBeNull();
    expect(getByText(user.clientAddress)).not.toBeNull();
    expect(getByText(user.lastActivity)).not.toBeNull();

    await act(() => mockAuthzRolesPromise);
  });

  describe('user settings', () => {
    it('should display timezone', async () => {
      const { getByText } = render(<UserDetails user={user} paginatedUserShares={undefined} />);

      expect(getByText(user.timezone)).not.toBeNull();

      await act(() => mockAuthzRolesPromise);
    });

    describe('should display session timeout in a readable format', () => {
      it('for seconds', async () => {
        const test = user.toBuilder().sessionTimeoutMs(10000).build();
        const { getByText } = render(<UserDetails user={test} paginatedUserShares={undefined} />);

        expect(getByText('10 Seconds')).not.toBeNull();

        await act(() => mockAuthzRolesPromise);
      });

      it('for minutes', async () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(600000).build()} paginatedUserShares={undefined} />);

        expect(getByText('10 Minutes')).not.toBeNull();

        await act(() => mockAuthzRolesPromise);
      });

      it('for hours', async () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(36000000).build()} paginatedUserShares={undefined} />);

        expect(getByText('10 Hours')).not.toBeNull();

        await act(() => mockAuthzRolesPromise);
      });

      it('for days', async () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(864000000).build()} paginatedUserShares={undefined} />);

        expect(getByText('10 Days')).not.toBeNull();

        await act(() => mockAuthzRolesPromise);
      });
    });
  });
});
