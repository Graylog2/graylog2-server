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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { fetchPeriodically } from 'logic/rest/FetchProvider';

import useNotificationBadgeCount from './useNotificationBadgeCount';

jest.mock('logic/rest/FetchProvider', () => ({
  __esModule: true,
  default: jest.fn(),
  fetchPeriodically: jest.fn(),
}));

const fetchPeriodicallyMock = fetchPeriodically as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationBadgeCount', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  it('calls /system/notifications/paginated with filters=is_read:false and per_page=1', async () => {
    fetchPeriodicallyMock.mockResolvedValue({
      pagination: { page: 1, per_page: 1, total: 7 },
      elements: [],
    });

    renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => {
      expect(fetchPeriodicallyMock).toHaveBeenCalled();
    });

    const [method, url] = fetchPeriodicallyMock.mock.calls[0];

    expect(method).toBe('GET');
    expect(url).toContain('/system/notifications/paginated');
    expect(url).toContain('per_page=1');
    // PaginationURL URI-encodes the colon: filters=is_read%3Afalse
    expect(url).toMatch(/filters=is_read(%3A|:)false/);
  });

  it('extracts data.pagination.total as the badge count', async () => {
    fetchPeriodicallyMock.mockResolvedValue({
      pagination: { page: 1, per_page: 1, total: 42 },
      elements: [],
    });

    const { result } = renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBe(42);
  });

  it('returns 0 when the response has no pagination.total (defensive default)', async () => {
    fetchPeriodicallyMock.mockResolvedValue({ elements: [] });

    const { result } = renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBe(0);
  });

  it('does not fetch when enabled=false (permission-gate)', () => {
    renderHook(() => useNotificationBadgeCount({ enabled: false }), { wrapper: buildWrapper(queryClient) });

    expect(fetchPeriodicallyMock).not.toHaveBeenCalled();
  });
});
