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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';

import EnrollmentTokenList from './EnrollmentTokenList';

import { fetchPaginatedEnrollmentTokens, useFleets, useCollectorsMutations } from '../hooks';
import type { EnrollmentTokenMetadata, Fleet } from '../types';

jest.mock('../hooks/useFleetQueries');
jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useEnrollmentTokenQueries', () => ({
  ...jest.requireActual('../hooks/useEnrollmentTokenQueries'),
  fetchPaginatedEnrollmentTokens: jest.fn(),
  enrollmentTokensKeyFn: jest.fn((params) => ['collectors', 'enrollment-tokens', 'paginated', params]),
}));

const mockFleets: Fleet[] = [
  { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z', target_version: '' },
  { id: 'fleet-2', name: 'Staging', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z', target_version: '' },
];

const futureDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
const pastDate = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

const mockToken = (overrides?: Partial<EnrollmentTokenMetadata>): EnrollmentTokenMetadata => ({
  id: 'token-1',
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
    { id: 'fleet_id', title: 'Fleet', sortable: false, filterable: true },
    { id: 'created_by', title: 'Created By', sortable: false },
    { id: 'created_at', title: 'Created At', sortable: true },
    { id: 'expires_at', title: 'Expires At', sortable: true },
    { id: 'usage_count', title: 'Usage Count', sortable: false },
    { id: 'last_used_at', title: 'Last Used', sortable: false },
  ],
});

describe('EnrollmentTokenList', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFleets).mockReturnValue({
      data: mockFleets,
      isLoading: false,
    } as unknown as ReturnType<typeof useFleets>);

    asMock(useCollectorsMutations).mockReturnValue({
      deleteEnrollmentToken: deleteEnrollmentTokenMock,
    } as unknown as ReturnType<typeof useCollectorsMutations>);

    asMock(fetchPaginatedEnrollmentTokens).mockResolvedValue(
      mockPaginatedResponse([mockToken()]),
    );
  });

  it('renders the enrollment tokens table', async () => {
    render(<EnrollmentTokenList />);

    await screen.findByText('admin');
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
      await screen.findByText(/re-enroll/i);
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
  });
});
