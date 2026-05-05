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
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import useNotificationConfig from './useNotificationConfig';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { getConfig: jest.fn() },
}));

jest.mock('logic/rest/FetchProvider', () => ({ __esModule: true, default: jest.fn() }));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), warning: jest.fn(), success: jest.fn() },
}));

const getConfigMock = SystemNotifications.getConfig as jest.Mock;
const fetchMock = fetch as jest.Mock;

const buildWrapper = (queryClient: QueryClient) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement);

  return Wrapper;
};

describe('useNotificationConfig', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
  });

  it('reads the current config (default 30)', async () => {
    getConfigMock.mockResolvedValue({ retention_days: 30 });

    const { result } = renderHook(() => useNotificationConfig(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.config).toEqual({ retention_days: 30 });
  });

  it('does not fetch the config when readEnabled=false (permission-gate)', () => {
    renderHook(() => useNotificationConfig({ readEnabled: false }), { wrapper: buildWrapper(queryClient) });
    expect(getConfigMock).not.toHaveBeenCalled();
  });

  it('sends PUT /system/notifications/config with body { retention_days: 90 } on update', async () => {
    getConfigMock.mockResolvedValue({ retention_days: 30 });
    fetchMock.mockResolvedValue({ retention_days: 90 });

    const { result } = renderHook(() => useNotificationConfig(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.update({ retention_days: 90 });
    });

    expect(fetchMock).toHaveBeenCalledWith(
      'PUT',
      '/system/notifications/config',
      { retention_days: 90 },
    );
  });

  it('rejects update locally when isUpdateEnabled=false (no network call)', async () => {
    getConfigMock.mockResolvedValue({ retention_days: 30 });

    const { result } = renderHook(
      () => useNotificationConfig({ updateEnabled: false }),
      { wrapper: buildWrapper(queryClient) },
    );

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await suppressConsole(async () => {
      await expect(result.current.update({ retention_days: 90 })).rejects.toBeDefined();
    });

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows a permission toast on 403 update failure', async () => {
    getConfigMock.mockResolvedValue({ retention_days: 30 });
    fetchMock.mockRejectedValue({ status: 403 });

    const { result } = renderHook(() => useNotificationConfig(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.update({ retention_days: 90 }).catch(() => {});
      });
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      expect.stringMatching(/permission/i),
      expect.any(String),
    );
  });

  it('does NOT toast on 400 (consumer renders inline validation)', async () => {
    getConfigMock.mockResolvedValue({ retention_days: 30 });
    fetchMock.mockRejectedValue({ status: 400 });

    const { result } = renderHook(() => useNotificationConfig(), { wrapper: buildWrapper(queryClient) });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await suppressConsole(async () => {
      await act(async () => {
        await result.current.update({ retention_days: -1 }).catch(() => {});
      });
    });

    expect(UserNotification.error).not.toHaveBeenCalled();
    // Wait for the mutation state to settle into the error.
    await waitFor(() => expect(result.current.updateError).toEqual({ status: 400 }));
  });
});
