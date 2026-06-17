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

import useNotificationBulkDismiss from './useNotificationBulkDismiss';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { bulkDelete: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), success: jest.fn(), warning: jest.fn() },
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const bulkDeleteMock = SystemNotifications.bulkDelete as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationBulkDismiss', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
  });

  it('calls bulkDelete with the given entity_ids', async () => {
    bulkDeleteMock.mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotificationBulkDismiss(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ entity_ids: ['row-1', 'row-2'] });
    });

    expect(bulkDeleteMock).toHaveBeenCalledWith({ entity_ids: ['row-1', 'row-2'] });
  });

  it('invalidates table and badge-count keys on success', async () => {
    bulkDeleteMock.mockResolvedValue(undefined);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationBulkDismiss(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ entity_ids: ['row-1'] });
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('shows a warning on 400', async () => {
    bulkDeleteMock.mockRejectedValue({ status: 400 });

    const { result } = renderHook(() => useNotificationBulkDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ entity_ids: [] }).catch(() => {});
      });
    });

    expect(UserNotification.warning).toHaveBeenCalledWith(
      expect.stringMatching(/no notifications selected/i),
      expect.any(String),
    );
  });

  it('shows an error toast on non-403/400 failures', async () => {
    bulkDeleteMock.mockRejectedValue({ status: 500 });

    const { result } = renderHook(() => useNotificationBulkDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ entity_ids: ['row-1'] }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/failed to dismiss/i),
      expect.any(String),
    );
  });

  it('skips toast and invalidation on 403', async () => {
    bulkDeleteMock.mockRejectedValue({ status: 403 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationBulkDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ entity_ids: ['row-1'] }).catch(() => {});
      });
    });

    expect(UserNotification.error).not.toHaveBeenCalled();
    expect(invalidateSpy).not.toHaveBeenCalled();
  });
});
