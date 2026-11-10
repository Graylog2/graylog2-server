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
import React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import BulkActions from './BulkActions';

import { useCollectorsMutations, useFleets } from '../hooks';
import { mockCollectorsMutations } from '../testing/mockMutations';
import type { Fleet } from '../types';

jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');
jest.mock('hooks/useCurrentUser');
jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useFleetQueries');

const userWith = (permissions: Array<string>) =>
  adminUser
    .toBuilder()
    .permissions(Immutable.List(permissions as Array<Permission>))
    .build();

const mockFleets: Fleet[] = [
  { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
  { id: 'fleet-2', name: 'Staging', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
];

const setSelectedEntitiesMock = jest.fn();
const bulkDeleteEnrollmentTokensMock = jest.fn(() =>
  Promise.resolve({ successfully_performed: 0, failures: [], errors: [] }),
);

const useSelectedEntitiesResponse = {
  selectedEntities: [],
  setSelectedEntities: setSelectedEntitiesMock,
  selectEntity: () => {},
  deselectEntity: () => {},
  toggleEntitySelect: () => {},
  isSomeRowsSelected: false,
  isAllRowsSelected: false,
};

const openActionsDropdown = async () => {
  await userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
};

describe('BulkActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSelectedEntities).mockReturnValue(useSelectedEntitiesResponse);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useFleets).mockReturnValue({ data: mockFleets, isLoading: false });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        bulkDeleteEnrollmentTokens: bulkDeleteEnrollmentTokensMock,
      }),
    );
  });

  it('renders bulk actions dropdown', async () => {
    render(<BulkActions />);

    await screen.findByRole('button', { name: /bulk actions/i });
  });

  it('is disabled when no entities are selected', async () => {
    render(<BulkActions />);

    const button = await screen.findByRole('button', { name: /bulk actions/i });

    expect(button).toBeDisabled();
  });

  it('is enabled when entities are selected', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1', 'token-2'],
    });

    render(<BulkActions />);

    const button = await screen.findByRole('button', { name: /bulk actions/i });

    expect(button).not.toBeDisabled();
  });

  it('shows Delete menu item', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1'],
    });

    render(<BulkActions />);

    await openActionsDropdown();

    await screen.findByRole('menuitem', { name: /delete/i });
  });

  it('shows confirmation dialog when Delete is clicked', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1', 'token-2'],
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

    await screen.findByText(/are you sure you want to delete 2 enrollment tokens/i);
  });

  it('calls bulkDeleteEnrollmentTokens when confirmed', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1', 'token-2'],
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

    expect(bulkDeleteEnrollmentTokensMock).toHaveBeenCalledWith(['token-1', 'token-2']);
  });

  it('clears selection after successful bulk delete', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1'],
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

    expect(setSelectedEntitiesMock).toHaveBeenCalledWith([]);
  });

  it('hides the dropdown when the user cannot delete tokens in any fleet', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1'],
    });
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_enrollment_tokens:read:fleet-1', 'collector_enrollment_tokens:read:fleet-2']),
    );

    render(<BulkActions />);

    expect(screen.queryByRole('button', { name: /bulk actions/i })).not.toBeInTheDocument();
  });

  it('shows the dropdown when the user can delete tokens in at least one fleet', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['token-1'],
    });
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_enrollment_tokens:read:fleet-2', 'collector_enrollment_tokens:delete:fleet-2']),
    );

    render(<BulkActions />);

    await screen.findByRole('button', { name: /bulk actions/i });
  });
});
