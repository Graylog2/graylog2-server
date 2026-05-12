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

import { SystemNotifications } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import useNotificationBadgeCount from './useNotificationBadgeCount';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { getPaginated: jest.fn() },
}));

const getPaginatedMock = SystemNotifications.getPaginated as jest.Mock;

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

  it('calls SystemNotifications.getPaginated with is_read:false filter and per_page=1', async () => {
    getPaginatedMock.mockResolvedValue({
      pagination: { page: 1, per_page: 1, total: 7 },
      elements: [],
      attributes: [],
    });

    renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => {
      expect(getPaginatedMock).toHaveBeenCalled();
    });

    expect(getPaginatedMock).toHaveBeenCalledWith(1, 1, undefined, ['is_read:false']);
  });

  it('extracts pagination.total as the badge count', async () => {
    asMock(getPaginatedMock).mockResolvedValue({
      pagination: { page: 1, per_page: 1, total: 42 },
      elements: [],
      attributes: [],
    });

    const { result } = renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBe(42);
  });

  it('returns 0 when the response has no pagination.total (defensive default)', async () => {
    asMock(getPaginatedMock).mockResolvedValue({ elements: [], attributes: [] });

    const { result } = renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBe(0);
  });

  it('does not fetch when enabled=false (permission-gate)', () => {
    renderHook(() => useNotificationBadgeCount({ enabled: false }), { wrapper: buildWrapper(queryClient) });

    expect(getPaginatedMock).not.toHaveBeenCalled();
  });
});
