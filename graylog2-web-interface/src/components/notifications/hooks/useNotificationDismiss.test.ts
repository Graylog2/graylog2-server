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

import useNotificationDismiss from './useNotificationDismiss';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { deleteNotificationById: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), success: jest.fn(), warning: jest.fn() },
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const deleteNotificationByIdMock = SystemNotifications.deleteNotificationById as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationDismiss', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
  });

  it('calls deleteNotificationById with the given id', async () => {
    deleteNotificationByIdMock.mockResolvedValue(undefined);

    const { result } = renderHook(() => useNotificationDismiss(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ id: 'row-1' });
    });

    expect(deleteNotificationByIdMock).toHaveBeenCalledWith('row-1');
  });

  it('invalidates table and badge-count keys on success', async () => {
    deleteNotificationByIdMock.mockResolvedValue(undefined);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationDismiss(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ id: 'row-1' });
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('silently invalidates table on 404 (notification already gone)', async () => {
    deleteNotificationByIdMock.mockRejectedValue({ status: 404 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1' }).catch(() => {});
      });
    });

    expect(UserNotification.error).not.toHaveBeenCalled();
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
  });

  it('shows an error toast on non-403/404 failures', async () => {
    deleteNotificationByIdMock.mockRejectedValue({ status: 500 });

    const { result } = renderHook(() => useNotificationDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1' }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/failed to dismiss/i),
      expect.any(String),
    );
  });

  it('skips toast and invalidation on 403', async () => {
    deleteNotificationByIdMock.mockRejectedValue({ status: 403 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationDismiss(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1' }).catch(() => {});
      });
    });

    expect(UserNotification.error).not.toHaveBeenCalled();
    expect(invalidateSpy).not.toHaveBeenCalled();
  });
});
