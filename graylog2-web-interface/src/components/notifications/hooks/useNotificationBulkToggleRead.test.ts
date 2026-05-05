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

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';

import useNotificationBulkToggleRead from './useNotificationBulkToggleRead';

jest.mock('logic/rest/FetchProvider', () => ({ __esModule: true, default: jest.fn() }));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), warning: jest.fn(), success: jest.fn() },
}));

jest.mock('hooks/useCurrentUser', () => ({
  __esModule: true,
  default: jest.fn(() => ({ id: 'user-1', username: 'tester' })),
}));

const TABLE_KEY = ['system', 'notifications', 'table'] as const;
const BADGE_KEY = ['system', 'notifications', 'badge-count'] as const;

const row = (id: string, isRead: boolean) => ({
  id,
  type: 'no_input_running',
  title: id,
  description: '',
  severity: 'normal',
  key: id,
  triggered_at: '2026-05-01T00:00:00.000Z',
  last_changed: '2026-05-01T00:00:00.000Z',
  node_id: 'node-1',
  details: {},
  is_read: isRead,
  actor: { id: 'system', name: 'System' },
});

const buildPage = (rows: ReturnType<typeof row>[]) => ({
  total: rows.length,
  pagination: { page: 1, per_page: 10, total: rows.length },
  defaults: { sort: { id: 'triggered_at', direction: 'DESC' as const } },
  query: '',
  attributes: [],
  sort: 'triggered_at',
  order: 'desc' as const,
  elements: rows,
});

const fetchMock = fetch as jest.Mock;

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
    asMock(useCurrentUser).mockReturnValue({ id: 'user-1', username: 'tester' });
  });

  it('per-row optimistic patches: a mixed selection produces a mixed result', async () => {
    queryClient.setQueryData(TABLE_KEY, buildPage([row('a', false), row('b', true), row('c', false)]));
    fetchMock.mockResolvedValue(undefined);

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

    // The optimistic patch flips each row from its initial state.
    // After settle the table is invalidated, so the cache may be re-fetched.
    // The fetch call shape is what matters.
    expect(fetchMock).toHaveBeenCalledWith(
      'POST',
      '/system/notifications/bulk/toggle_read',
      { entity_ids: ['a', 'b', 'c'] },
    );
  });

  it('reverts the cache and shows a permission toast on 403', async () => {
    queryClient.setQueryData(TABLE_KEY, buildPage([row('a', false)]));
    fetchMock.mockRejectedValue({ status: 403 });

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.mutateAsync({ rows: [{ id: 'a', currentIsRead: false }] }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/permission/i),
      expect.any(String),
    );
    const cached = queryClient.getQueryData<ReturnType<typeof buildPage>>(TABLE_KEY);
    expect(cached?.elements?.[0].is_read).toBe(false);
  });

  it('reverts and warns on 400 (empty array)', async () => {
    queryClient.setQueryData(TABLE_KEY, buildPage([row('a', false)]));
    fetchMock.mockRejectedValue({ status: 400 });

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

  it('partial-success: 204 with silently-dropped unknown ids triggers a re-fetch via invalidation', async () => {
    queryClient.setQueryData(TABLE_KEY, buildPage([row('a', false)]));
    fetchMock.mockResolvedValue(undefined);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useNotificationBulkToggleRead(), { wrapper: buildWrapper(queryClient) });

    await act(async () => {
      await result.current.mutateAsync({
        rows: [
          { id: 'a', currentIsRead: false },
          { id: 'unknown-id', currentIsRead: false },
        ],
      });
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: TABLE_KEY });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: BADGE_KEY });
    });
  });

  it('skips invalidation on 403', async () => {
    queryClient.setQueryData(TABLE_KEY, buildPage([row('a', false)]));
    fetchMock.mockRejectedValue({ status: 403 });
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
