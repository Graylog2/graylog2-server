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
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import EnrollmentTokenList from './EnrollmentTokenList';

import { fetchPaginatedEnrollmentTokens, useFleets, useCollectorsMutations } from '../hooks';
import type { EnrollmentTokenMetadata, Fleet } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('hooks/useCurrentUser');
jest.mock('../hooks/useFleetQueries');
jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useEnrollmentTokenQueries', () => ({
  ...jest.requireActual('../hooks/useEnrollmentTokenQueries'),
  fetchPaginatedEnrollmentTokens: jest.fn(),
  enrollmentTokensKeyFn: jest.fn((params) => ['collectors', 'enrollment-tokens', 'paginated', params]),
}));

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

const mockFleets: Fleet[] = [
  {
    id: 'fleet-1',
    name: 'Production',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
  {
    id: 'fleet-2',
    name: 'Staging',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
  },
];

const futureDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
const pastDate = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

const mockToken = (overrides?: Partial<EnrollmentTokenMetadata>): EnrollmentTokenMetadata => ({
  id: 'token-1',
  name: 'Test token',
  jti: 'jti-1',
  kid: 'kid-1',
  fleet_id: 'fleet-1',
  created_by: { user_id: 'user-1', username: 'admin' },
  created_at: '2026-03-01T00:00:00Z',
  expires_at: futureDate,
  usage_count: 3,
  last_used_at: '2026-03-15T12:00:00Z',
  ...overrides,
});

const deleteEnrollmentTokenMock = jest.fn(() => Promise.resolve());

const mockPaginatedResponse = (tokens: EnrollmentTokenMetadata[]) => ({
  list: tokens,
  pagination: { total: tokens.length },
  attributes: [
    { id: 'name', title: 'Name', sortable: true, filterable: true, searchable: true },
    { id: 'fleet_id', title: 'Fleet', sortable: false, filterable: true },
    { id: 'created_by', title: 'Created By', sortable: false },
    { id: 'created_at', title: 'Created At', sortable: true },
    { id: 'expires_at', title: 'Expires At', sortable: true },
    { id: 'usage_count', title: 'Usage Count', sortable: false },
    { id: 'last_used_at', title: 'Last Used', sortable: false },
  ],
});

describe('EnrollmentTokenList', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    });

    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        deleteEnrollmentToken: deleteEnrollmentTokenMock,
      }),
    );

    asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(mockPaginatedResponse([mockToken()]));
  });

  it('renders the enrollment tokens table', async () => {
    render(<EnrollmentTokenList />);

    await screen.findByText('admin');
  });

  it('renders the token name', async () => {
    render(<EnrollmentTokenList />);

    await screen.findByText('Test token');
  });

  it('resolves fleet name from fleet_id', async () => {
    render(<EnrollmentTokenList />);

    await screen.findByText('Production');
  });

  it('shows usage count', async () => {
    render(<EnrollmentTokenList />);

    await screen.findByText('3');
  });

  describe('expiry display', () => {
    it('shows "Never" for tokens without expiry', async () => {
      asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
        mockPaginatedResponse([mockToken({ expires_at: null })]),
      );

      render(<EnrollmentTokenList />);

      await screen.findByText('Never');
    });

    it('shows "Expired" for tokens past their expiry', async () => {
      asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
        mockPaginatedResponse([mockToken({ expires_at: pastDate })]),
      );

      render(<EnrollmentTokenList />);

      await screen.findByText('Expired');
    });
  });

  describe('delete action', () => {
    const openMoreActions = async () => {
      await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
    };

    it('shows delete menu item in more actions', async () => {
      render(<EnrollmentTokenList />);

      await openMoreActions();

      await screen.findByRole('menuitem', { name: /delete/i });
    });

    it('shows confirmation dialog when delete is clicked', async () => {
      render(<EnrollmentTokenList />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

      await screen.findByText(/are you sure you want to delete this enrollment token/i);
      await screen.findByText(/continue to operate normally/i);
    });

    it('calls deleteEnrollmentToken when confirmed', async () => {
      render(<EnrollmentTokenList />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
      await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(deleteEnrollmentTokenMock).toHaveBeenCalledWith('token-1');
      });
    });

    it('emits DELETED telemetry when token is deleted', async () => {
      render(<EnrollmentTokenList />);

      await openMoreActions();
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
      await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(
          'Collector Enrollment Token Deleted',
          expect.objectContaining({
            app_action_value: 'token-delete',
          }),
        );
        expect(sendTelemetry).not.toHaveBeenCalledWith(
          'Collector Enrollment Token Deleted',
          expect.objectContaining({ token_id: expect.anything() }),
        );
      });
    });
  });

  describe('bulk delete action', () => {
    it('emits BULK_DELETED telemetry with count on multi-select delete', async () => {
      const bulkDeleteMock = jest.fn(() => Promise.resolve());
      asMock(useCollectorsMutations).mockReturnValue(
        mockCollectorsMutations({
          deleteEnrollmentToken: deleteEnrollmentTokenMock,
          bulkDeleteEnrollmentTokens: bulkDeleteMock as any,
        }),
      );

      asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
        mockPaginatedResponse([
          mockToken({ id: 'token-1', name: 'Token 1' }),
          mockToken({ id: 'token-2', name: 'Token 2' }),
        ]),
      );

      render(<EnrollmentTokenList />);

      await waitFor(() => {
        expect(screen.getByText('Token 1')).toBeInTheDocument();
        expect(screen.getByText('Token 2')).toBeInTheDocument();
      });

      // Select multiple tokens via checkboxes
      const checkboxes = await screen.findAllByRole('checkbox');
      // Skip header checkbox (index 0)
      await userEvent.click(checkboxes[1]);
      await userEvent.click(checkboxes[2]);

      // Open bulk actions and delete
      const bulkActionButton = await screen.findByRole('button', { name: /bulk actions/i });
      await userEvent.click(bulkActionButton);
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
      await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(sendTelemetry).toHaveBeenCalledWith(
          'Collector Enrollment Tokens Bulk Deleted',
          expect.objectContaining({
            count: 2,
            app_action_value: 'token-bulk-delete',
          }),
        );
      });
    });
  });
});

describe('EnrollmentTokenList permissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    });

    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        deleteEnrollmentToken: deleteEnrollmentTokenMock,
      }),
    );

    asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
      mockPaginatedResponse([mockToken({ id: 'token-1', fleet_id: 'fleet-1' })]),
    );
  });

  it('hides the delete action without token delete on the token fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_enrollment_tokens:read:fleet-1']));

    render(<EnrollmentTokenList />);

    await screen.findByText('Test token');

    expect(screen.queryByRole('button', { name: /more actions/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /delete/i })).not.toBeInTheDocument();
  });

  it('shows the delete action when token delete is permitted on the token fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_enrollment_tokens:read:fleet-1', 'collector_enrollment_tokens:delete:fleet-1']),
    );

    render(<EnrollmentTokenList />);

    await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));

    await screen.findByRole('menuitem', { name: /delete/i });
  });

  it('disables bulk selection for a token the user may not delete', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_enrollment_tokens:read:fleet-1']));

    render(<EnrollmentTokenList />);

    await screen.findByText('Test token');

    expect(await screen.findByRole('checkbox', { name: /select entity/i })).toBeDisabled();
  });

  it('enables bulk selection for a token the user may delete', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_enrollment_tokens:read:fleet-1', 'collector_enrollment_tokens:delete:fleet-1']),
    );

    render(<EnrollmentTokenList />);

    await screen.findByText('Test token');

    expect(await screen.findByRole('checkbox', { name: /select entity/i })).toBeEnabled();
  });

  // The selection spans fleets, so the gate has to be per row rather than per table: the backend
  // filters a bulk delete down to the permitted tokens, and the checkboxes must say the same.
  it('gates bulk selection per row when delete is granted on only one fleet', async () => {
    asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
      mockPaginatedResponse([
        mockToken({ id: 'token-1', name: 'Token 1', fleet_id: 'fleet-1' }),
        mockToken({ id: 'token-2', name: 'Token 2', fleet_id: 'fleet-2' }),
      ]),
    );
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_enrollment_tokens:read', 'collector_enrollment_tokens:delete:fleet-1']),
    );

    render(<EnrollmentTokenList />);

    await screen.findByText('Token 1');

    const [fleetOneCheckbox, fleetTwoCheckbox] = await screen.findAllByRole('checkbox', { name: /select entity/i });

    expect(fleetOneCheckbox).toBeEnabled();
    expect(fleetTwoCheckbox).toBeDisabled();
  });
});
