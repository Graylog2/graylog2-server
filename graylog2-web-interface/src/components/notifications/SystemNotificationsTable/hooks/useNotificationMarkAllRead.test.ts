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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { SystemNotifications } from '@graylog/server-api';

import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';

import useNotificationMarkAllRead from './useNotificationMarkAllRead';

// The confirmation-modal-precedes-API-call assertion lives in the Phase 2
// MarkAllAsReadConfirmationModal test (tasks.md:2.10). This test focuses on
// the hook's own contract: 204 → invalidate both prefixes; 403 → toast +
// skip invalidation.
jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { readAll: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), warning: jest.fn(), success: jest.fn() },
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const readAllMock = SystemNotifications.readAll as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationMarkAllRead', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
  });

  it('invalidates BOTH the table prefix and the badge-count prefix on success', async () => {
    readAllMock.mockResolvedValue(undefined);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationMarkAllRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync();
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('shows a permission toast and skips invalidation on 403', async () => {
    readAllMock.mockRejectedValue({ status: 403 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationMarkAllRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync().catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/permission/i),
      expect.any(String),
    );
    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it('shows a generic toast on non-403 errors', async () => {
    readAllMock.mockRejectedValue({ status: 500 });

    const { result } = renderHook(() => useNotificationMarkAllRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync().catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/mark all/i),
      expect.any(String),
    );
  });
});
