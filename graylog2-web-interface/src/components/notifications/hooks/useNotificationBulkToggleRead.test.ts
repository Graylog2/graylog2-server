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

import useNotificationBulkToggleRead from './useNotificationBulkToggleRead';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { bulkToggleRead: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), warning: jest.fn(), success: jest.fn() },
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const bulkToggleReadMock = SystemNotifications.bulkToggleRead as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationBulkToggleRead', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
  });

  it('calls bulkToggleRead with the given entity ids', async () => {
    bulkToggleReadMock.mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({
        rows: [
          { id: 'a', currentIsRead: false },
          { id: 'b', currentIsRead: true },
          { id: 'c', currentIsRead: false },
        ],
      });
    });

    expect(bulkToggleReadMock).toHaveBeenCalledWith({ entity_ids: ['a', 'b', 'c'] });
  });

  it('invalidates table and badge-count keys on success', async () => {
    bulkToggleReadMock.mockResolvedValue(undefined);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ rows: [{ id: 'a', currentIsRead: false }] });
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('skips toast on 403', async () => {
    bulkToggleReadMock.mockRejectedValue({ status: 403 });

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ rows: [{ id: 'a', currentIsRead: false }] }).catch(() => {});
      });
    });

    expect(UserNotification.error).not.toHaveBeenCalled();
    expect(UserNotification.warning).not.toHaveBeenCalled();
  });

  it('shows "no notifications selected" warning on 400', async () => {
    bulkToggleReadMock.mockRejectedValue({ status: 400 });

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ rows: [] }).catch(() => {});
      });
    });

    expect(UserNotification.warning).toHaveBeenCalledWith(
      expect.stringMatching(/no notifications selected/i),
      expect.stringMatching(/nothing to update/i),
    );
  });

  it('shows an error toast on non-403 failures', async () => {
    bulkToggleReadMock.mockRejectedValue({ status: 500 });

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ rows: [{ id: 'a', currentIsRead: false }] }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(expect.stringMatching(/failed to update/i), expect.any(String));
  });

  it('skips invalidation on 403', async () => {
    bulkToggleReadMock.mockRejectedValue({ status: 403 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ rows: [{ id: 'a', currentIsRead: false }] }).catch(() => {});
      });
    });

    expect(invalidateSpy).not.toHaveBeenCalled();
  });
});
