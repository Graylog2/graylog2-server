// @flow strict
import * as React from 'react';
import { render, act, waitFor, fireEvent } from 'wrappedTestingLibrary';
import mockAction from 'helpers/mocking/MockAction';
import { rolesList as mockRoles } from 'fixtures/roles';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RolesOverview from './RolesOverview';

const loadRolesPaginatedResponse = {
  list: mockRoles,
  pagination: {
    page: 1,
    perPage: 10,
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
  describe('should display table headers', () => {
    const displaysHeader = async ({ header }) => {
      const { queryByText } = render(<RolesOverview />);
      await act(() => mockLoadRolesPaginatedPromise);

      expect(queryByText(header)).not.toBeNull();
    };

    it.each`
      header
      ${'Name'}
      ${'Description'}
      ${'Actions'}
    `('$header', displaysHeader);
  });

  it('should fetch and list roles with name and description', async () => {
    const { queryByText } = render(<RolesOverview />);
    await act(() => mockLoadRolesPaginatedPromise);

    expect(queryByText(mockRoles.first().name)).not.toBeNull();
    expect(queryByText(mockRoles.first().description)).not.toBeNull();
  });

  it('should allow searching for roles', async () => {
    const { getByPlaceholderText, getByRole } = render(<RolesOverview />);
    await act(() => mockLoadRolesPaginatedPromise);

    const searchInput = getByPlaceholderText('Enter search query...');
    const searchSubmitButton = getByRole('button', { name: 'Search' });

    fireEvent.change(searchInput, { target: { value: 'name:manager' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith(1, 10, 'name:manager'));
  });

  it('should reset search', async () => {
    const { getByPlaceholderText, getByRole } = render(<RolesOverview />);
    await act(() => mockLoadRolesPaginatedPromise);
    const searchSubmitButton = getByRole('button', { name: 'Search' });
    const resetSearchButton = getByRole('button', { name: 'Reset' });
    const searchInput = getByPlaceholderText('Enter search query...');

    fireEvent.change(searchInput, { target: { value: 'name:manager' } });
    fireEvent.click(searchSubmitButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith(1, 10, 'name:manager'));

    fireEvent.click(resetSearchButton);

    await waitFor(() => expect(AuthzRolesActions.loadRolesPaginated).toHaveBeenCalledWith(1, 10, ''));
  });
});
