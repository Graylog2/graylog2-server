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
import React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

import suppressConsole from 'helpers/suppressConsole';
import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { EventType } from 'hooks/useEventById';
import useEventById, { eventsUrl } from 'hooks/useEventById';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});
const wrapper = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);
const url = eventsUrl('111');

const mockData = { event: { id: '111' } as EventType };

describe('useEventById', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should run fetch and store mapped response', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve({ event: { id: '111' } }));
    const { result, waitFor } = renderHook(() => useEventById('111'), { wrapper });

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(fetch).toHaveBeenCalledWith('GET', url);
    expect(result.current.data).toEqual(mockData.event);
  });

  it('should display notification on fail', async () => {
    await suppressConsole(async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { waitFor } = renderHook(() => useEventById('111'), { wrapper });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Loading event or alert failed with status: Error: Error',
        'Could not load event or alert'));
    });
  });
});
