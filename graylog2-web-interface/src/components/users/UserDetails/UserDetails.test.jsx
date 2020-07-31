// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, act, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { simplePaginatedUserShares } from 'fixtures/userEntityShares';
import selectEvent from 'react-select-event';

import { EntityShareActions } from 'stores/permissions/EntityShareStore';
import User from 'logic/users/User';

import UserDetails from './UserDetails';

const mockAuthzRolesPromise = Promise.resolve({ list: Immutable.List(), pagination: { total: 0 } });
const mockPaginatedUserShares = simplePaginatedUserShares(1, 10, '');

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesActions: {
    loadForUser: jest.fn(() => mockAuthzRolesPromise),
    loadPaginated: jest.fn(() => mockAuthzRolesPromise),
  },
}));

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    searchPaginatedUserShares: jest.fn(() => Promise.resolve(mockPaginatedUserShares)),
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
  afterEach(() => {
    jest.clearAllMocks();
  });

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

    describe('shared entities section', () => {
      it('should list provided paginated user shares', async () => {
        const { getAllByText } = render(<UserDetails user={user} paginatedUserShares={mockPaginatedUserShares} />);

        expect(getAllByText(mockPaginatedUserShares.list.first().title)).not.toBeNull();

        await act(() => mockAuthzRolesPromise);
      });

      it('should fetch paginated user shares when using search', async () => {
        const { getByPlaceholderText, getByText } = render(<UserDetails user={user} paginatedUserShares={mockPaginatedUserShares} />);

        const searchInput = getByPlaceholderText('Filter by name');
        const searchSubmitButton = getByText('Search');

        fireEvent.change(searchInput, { target: { value: 'the username' } });
        fireEvent.click(searchSubmitButton);

        await waitFor(() => expect(EntityShareActions.searchPaginatedUserShares).toHaveBeenCalledWith(user.username, 1, 10, 'the username', undefined));

        await act(() => mockAuthzRolesPromise);
      });

      it('should fetch user shares when filtering by entity type', async () => {
        const existingPaginatedUserShares = { ...mockPaginatedUserShares, pagination: { ...mockPaginatedUserShares.pagination, page: 3, perPage: 50, query: 'existing query' } };
        const { getByLabelText } = render(<UserDetails user={user} paginatedUserShares={existingPaginatedUserShares} />);

        const entityTypeSelect = getByLabelText('Entity Type');
        await selectEvent.openMenu(entityTypeSelect);
        await act(async () => { await selectEvent.select(entityTypeSelect, 'Dashboard'); });

        expect(EntityShareActions.searchPaginatedUserShares).toHaveBeenCalledWith(user.username, 1, 50, 'existing query', { entity_type: 'dashboard' });

        await act(() => mockAuthzRolesPromise);
      });

      it('should fetch user shares when filtering by capability', async () => {
        const existingPaginatedUserShares = { ...mockPaginatedUserShares, pagination: { ...mockPaginatedUserShares.pagination, page: 3, perPage: 50, query: 'existing query' } };
        const { getByLabelText } = render(<UserDetails user={user} paginatedUserShares={existingPaginatedUserShares} />);

        const capabilitySelect = getByLabelText('Capability');
        await selectEvent.openMenu(capabilitySelect);
        await act(async () => { await selectEvent.select(capabilitySelect, 'Manager'); });

        expect(EntityShareActions.searchPaginatedUserShares).toHaveBeenCalledWith(user.username, 1, 50, 'existing query', { capability: 'manage' });

        await act(() => mockAuthzRolesPromise);
      });
    });
  });
});
