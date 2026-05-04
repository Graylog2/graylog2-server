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

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';

import useNotificationToggleRead from './useNotificationToggleRead';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { toggleRead: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), success: jest.fn(), warning: jest.fn() },
}));

jest.mock('hooks/useCurrentUser', () => ({
  __esModule: true,
  default: jest.fn(() => ({ id: 'user-1', username: 'tester' })),
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const baseRow = {
  id: 'row-1',
  type: 'no_input_running',
  title: 'Title',
  description: 'desc',
  severity: 'normal',
  key: 'k',
  triggered_at: '2026-05-01T00:00:00.000Z',
  last_changed: '2026-05-01T00:00:00.000Z',
  node_id: 'node-1',
  details: {},
  is_read: false,
  actor: { id: 'system', name: 'System' },
};

const buildPage = (rows = [baseRow]) => ({
  total: rows.length,
  pagination: { page: 1, per_page: 10, total: rows.length },
  defaults: { sort: { id: 'triggered_at', direction: 'DESC' as const } },
  query: '',
  attributes: [],
  sort: 'triggered_at',
  order: 'desc' as const,
  elements: rows,
});

const toggleReadMock = SystemNotifications.toggleRead as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationToggleRead', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    queryClient.setQueryData(TABLE_KEY, buildPage());
    asMock(useCurrentUser).mockReturnValue({ id: 'user-1', username: 'tester' });
  });

  it('optimistic-patches the table cache and reconciles with the server entity on success', async () => {
    const serverEntity = {
      ...baseRow,
      is_read: true,
      last_changed: '2026-05-01T00:01:00.000Z',
      actor: { id: 'server', name: 'ServerOverride' },
    };

    toggleReadMock.mockResolvedValue(serverEntity);

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ id: 'row-1', currentIsRead: false });
    });

    const cached = queryClient.getQueryData<ReturnType<typeof buildPage>>(TABLE_KEY);
    expect(cached?.elements?.[0]).toEqual(serverEntity);
  });

  it('reverts the cache and shows a permission toast on 403', async () => {
    toggleReadMock.mockRejectedValue({ status: 403 });

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1', currentIsRead: false }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(expect.stringMatching(/permission/i), expect.any(String));
    const cached = queryClient.getQueryData<ReturnType<typeof buildPage>>(TABLE_KEY);
    expect(cached?.elements?.[0].is_read).toBe(false);
  });

  it('reverts the cache and shows a "no longer exists" toast on 404, and invalidates the table', async () => {
    toggleReadMock.mockRejectedValue({ status: 404 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1', currentIsRead: false }).catch(() => {});
      });
    });

    expect(UserNotification.warning).toHaveBeenCalledWith(expect.stringMatching(/no longer exists/i), expect.any(String));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
  });

  it('invalidates BOTH the table prefix and the badge-count prefix on success', async () => {
    toggleReadMock.mockResolvedValue({ ...baseRow, is_read: true });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ id: 'row-1', currentIsRead: false });
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('skips invalidation on 403 (server state unchanged)', async () => {
    toggleReadMock.mockRejectedValue({ status: 403 });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ id: 'row-1', currentIsRead: false }).catch(() => {});
      });
    });

    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it('records the actor as the current user in the optimistic patch', async () => {
    let capturedDuringFlight: typeof baseRow | undefined;

    toggleReadMock.mockImplementation(() => {
      capturedDuringFlight = queryClient.getQueryData<ReturnType<typeof buildPage>>(TABLE_KEY)?.elements?.[0];

      return Promise.resolve({ ...baseRow, is_read: true });
    });

    const { result } = renderHook(() => useNotificationToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({ id: 'row-1', currentIsRead: false });
    });

    expect(capturedDuringFlight?.is_read).toBe(true);
    expect(capturedDuringFlight?.actor).toEqual({ id: 'user-1', name: 'tester' });
  });
});
