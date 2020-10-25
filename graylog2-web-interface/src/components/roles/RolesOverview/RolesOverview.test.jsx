// @flow strict
import * as React from 'react';
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';
import mockAction from 'helpers/mocking/MockAction';
import { rolesList as mockRoles } from 'fixtures/roles';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RolesOverview from './RolesOverview';

const loadRolesPaginatedResponse = {
  list: mockRoles,
  pagination: {
    page: 1,
    perPage: 10,
    query: '',
    count: mockRoles.size,
    total: mockRoles.size,
  },
};

const mockLoadRolesPaginatedPromise = Promise.resolve(loadRolesPaginatedResponse);

jest.mock('stores/roles/AuthzRolesStore', () => ({
  AuthzRolesStore: {
    listen: jest.fn(),
  },
  AuthzRolesActions: {
    delete: mockAction(jest.fn(() => Promise.resolve())),
    loadRolesPaginated: jest.fn(() => mockLoadRolesPaginatedPromise),
  },
}));

describe('RolesOverview', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display table header', async () => {
    render(<RolesOverview />);
    const headers = [
      'Name',
      'Description',
      'Actions',
    ];

    // wait until list is displayed
    await screen.findByText('Roles');

    headers.forEach((header) => {
      expect(screen.getByText(header)).toBeInTheDocument();
    });
  });

  it('should fetch and list roles with name and description', async () => {
    render(<RolesOverview />);

    await screen.findByText(mockRoles.first().name);

    expect(screen.queryByText(mockRoles.first().description)).toBeInTheDocument();
  });

  it('should allow searching for roles', async () => {
    render(<RolesOverview />);

    const searchInput = await screen.findByPlaceholderText('Enter search query...');
    const searchSubmitButton = screen.getByRole('button', { name: 'Search' });

    fireEvent.change(searchInput, { target: { value: 'name:manager' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith({ page: 1, perPage: 10, query: 'name:manager' }));
  });

  it('should reset search', async () => {
    render(<RolesOverview />);

    const searchSubmitButton = await screen.findByRole('button', { name: 'Search' });
    const resetSearchButton = screen.getByRole('button', { name: 'Reset' });
    const searchInput = screen.getByPlaceholderText('Enter search query...');

    fireEvent.change(searchInput, { target: { value: 'name:manager' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith({ page: 1, perPage: 10, query: 'name:manager' }));

    fireEvent.click(resetSearchButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith({ page: 1, perPage: 10, query: '' }));
  });
});
