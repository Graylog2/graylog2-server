// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
// import { render, act, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { paginatedShares } from 'fixtures/sharedEntities';
// import selectEvent from 'react-select-event';
import { reader as assignedRole } from 'fixtures/roles';
import { admin as currentUser } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';
// import { EntityShareActions } from 'stores/permissions/EntityShareStore';
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
  .username('The username')
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

describe('<UserDetails />', () => {
  const SutComponent = (props) => (
    <CurrentUserContext.Provider value={{ ...currentUser, permissions: ['*'] }}>
      <UserDetails {...props} />
    </CurrentUserContext.Provider>
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('user profile should display profile information', async () => {
    render(<SutComponent user={user} />);

    await screen.findByText(user.username);

    expect(screen.getByText(user.fullName)).toBeInTheDocument();
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

        await waitFor(() => expect(screen.getByText('10 Seconds')).toBeInTheDocument());
      });

      it('for minutes', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(600000).build()} />);

        await waitFor(() => expect(screen.getByText('10 Minutes')).toBeInTheDocument());
      });

      it('for hours', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(36000000).build()} />);

        await waitFor(() => expect(screen.getByText('10 Hours')).toBeInTheDocument());
      });

      it('for days', async () => {
        render(<SutComponent user={user.toBuilder().sessionTimeoutMs(864000000).build()} />);

        await waitFor(() => expect(screen.getByText('10 Days')).toBeInTheDocument());
      });
    });

    // describe('shared entities section', () => {
    //   it('should list provided paginated user shares', async () => {
    //     const { getAllByText } = render(<SutComponent user={user} paginatedUserShares={mockPaginatedUserShares} />);
    //     await act(() => mockAuthzRolesPromise);

    //     expect(getAllByText(mockPaginatedUserShares.list.first().title)).toBeInTheDocument();
    //   });

    //   it('should fetch paginated user shares when using search', async () => {
    //     const { getByPlaceholderText, getByText } = render(<SutComponent user={user} paginatedUserShares={mockPaginatedUserShares} />);
    //     await act(() => mockAuthzRolesPromise);

    //     const searchInput = getByPlaceholderText('Enter search query...');
    //     const searchSubmitButton = screen.getByText('Search');

    //     fireEvent.change(searchInput, { target: { value: 'the username' } });
    //     fireEvent.click(searchSubmitButton);

    //     await waitFor(() => expect(EntityShareActions.loadUserSharesPaginated).toHaveBeenCalledWith(user.username, 1, 10, 'the username', undefined));
    //   });

    //   it('should fetch user shares when filtering by entity type', async () => {
    //     const existingPaginatedUserShares = { ...mockPaginatedUserShares, pagination: { ...mockPaginatedUserShares.pagination, page: 1, perPage: 50, query: 'existing query' } };
    //     const { getByLabelText } = render(<SutComponent user={user} paginatedUserShares={existingPaginatedUserShares} />);
    //     await act(() => mockAuthzRolesPromise);

    //     const entityTypeSelect = getByLabelText('Entity Type');
    //     await selectEvent.openMenu(entityTypeSelect);
    //     await act(async () => { await selectEvent.select(entityTypeSelect, 'Dashboard'); });

    //     expect(EntityShareActions.loadUserSharesPaginated).toHaveBeenCalledWith(user.username, 1, 50, 'existing query', { entity_type: 'dashboard' });
    //   });

    //   it('should fetch user shares when filtering by capability', async () => {
    //     const existingPaginatedUserShares = { ...mockPaginatedUserShares, pagination: { ...mockPaginatedUserShares.pagination, page: 1, perPage: 50, query: 'existing query' } };
    //     const { getByLabelText } = render(<SutComponent user={user} paginatedUserShares={existingPaginatedUserShares} />);
    //     await act(() => mockAuthzRolesPromise);

    //     const capabilitySelect = getByLabelText('Capability');
    //     await selectEvent.openMenu(capabilitySelect);
    //     await act(async () => { await selectEvent.select(capabilitySelect, 'Manager'); });

    //     expect(EntityShareActions.loadUserSharesPaginated).toHaveBeenCalledWith(user.username, 1, 50, 'existing query', { capability: 'manage' });
    //   });
    // });
  });

  describe('roles section', () => {
    it('should display assigned roles', async () => {
      render(<SutComponent user={user} />);

      await waitFor(() => expect(screen.getByText(assignedRole.name)).toBeInTheDocument());
    });
  });

  describe('teams section', () => {
    it('should display info if license is not present', async () => {
      render(<SutComponent user={user} />);

      await waitFor(() => expect(screen.getByText(/Enterprise Feature/)).toBeInTheDocument());
    });
  });
});
