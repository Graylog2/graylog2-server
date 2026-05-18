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
  SystemNotifications: { getUnreadCount: jest.fn() },
}));

const getUnreadCountMock = asMock(SystemNotifications.getUnreadCount);

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

  it('calls SystemNotifications.getUnreadCount with no session extension', async () => {
    getUnreadCountMock.mockResolvedValue(7);

    renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => {
      expect(getUnreadCountMock).toHaveBeenCalled();
    });

    expect(getUnreadCountMock).toHaveBeenCalledWith({ requestShouldExtendSession: false });
  });

  it('returns the count from getUnreadCount', async () => {
    getUnreadCountMock.mockResolvedValue(42);

    const { result } = renderHook(() => useNotificationBadgeCount(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBe(42);
  });

  it('does not fetch when enabled=false (permission-gate)', () => {
    renderHook(() => useNotificationBadgeCount({ enabled: false }), { wrapper: buildWrapper(queryClient) });

    expect(getUnreadCountMock).not.toHaveBeenCalled();
  });

});
