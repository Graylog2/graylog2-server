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
import { renderHook } from '@testing-library/react-hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import { qualifyUrl } from 'util/URLUtils';
import { urlPrefix, useLastOpened, usePinnedItems, useRecentActivity } from 'components/welcome/hooks';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';

const getUrl = (url) => qualifyUrl(`${urlPrefix}/${url}?page=1&per_page=5`);
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

const mockLastOpened = {
  lastOpened: [{
    type: 'search',
    id: '1',
    title: 'Title 1',
  }],
  page: 1,
};

const mockPinnedItems = {
  lastOpened: [{
    type: 'search',
    id: '1',
    title: 'Title 1',
  }],
  page: 1,
};

const mockedRecentActivityResponse = {
  recentActivity: [{
    id: '5',
    activity_type: 'share',
    item_type: 'dashboard',
    item_id: '5',
    title: 'Title 5',
    timestamp: '2022-01-01',
  }],
  page: 1,
  total: 1,
};

const mockedRecentActivity = {
  recentActivity: [{
    id: '5',
    activityType: 'share',
    itemType: 'dashboard',
    itemId: '5',
    title: 'Title 5',
    timestamp: '2022-01-01',
  }],
  page: 1,
  total: 1,
};

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

describe('Hooks for welcome page', () => {
  describe('useLastOpened custom hook', () => {
    afterEach(() => {
      jest.clearAllMocks();
    });

    it('Test return initial data and take from fetch', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve(mockLastOpened));
      const { result, waitFor } = renderHook(() => useLastOpened(DEFAULT_PAGINATION), { wrapper });

      await waitFor(() => result.current.isFetching);
      await waitFor(() => !result.current.isFetching);

      expect(fetch).toHaveBeenCalledWith('GET', getUrl('lastOpened'));
      expect(result.current.data).toEqual(mockLastOpened);
    });

    it('Test trigger notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useLastOpened(DEFAULT_PAGINATION), { wrapper });

      await suppressConsole(async () => {
        await waitFor(() => result.current.isFetching);
        await waitFor(() => !result.current.isFetching);
      });

      expect(UserNotification.error).toHaveBeenCalledWith(
        'Loading last opened items failed with status: Error: Error',
        'Could not load last opened items');
    });
  });

  describe('usePinned custom hook', () => {
    afterEach(() => {
      jest.clearAllMocks();
    });

    it('Test return initial data and take from fetch', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve(mockPinnedItems));
      const { result, waitFor } = renderHook(() => usePinnedItems(DEFAULT_PAGINATION), { wrapper });

      await waitFor(() => result.current.isFetching);
      await waitFor(() => !result.current.isFetching);

      expect(fetch).toHaveBeenCalledWith('GET', getUrl('pinnedItems'));
      expect(result.current.data).toEqual(mockLastOpened);
    });

    it('Test trigger notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => usePinnedItems(DEFAULT_PAGINATION), { wrapper });

      await suppressConsole(async () => {
        await waitFor(() => result.current.isFetching);
        await waitFor(() => !result.current.isFetching);
      });

      expect(UserNotification.error).toHaveBeenCalledWith(
        'Loading pinned items failed with status: Error: Error',
        'Could not load pinned items');
    });
  });

  describe('useRecentActivities custom hook', () => {
    afterEach(() => {
      jest.clearAllMocks();
    });

    it('Test return initial data and take from fetch', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve(mockedRecentActivityResponse));
      const { result, waitFor } = renderHook(() => useRecentActivity(DEFAULT_PAGINATION), { wrapper });

      await waitFor(() => result.current.isFetching);
      await waitFor(() => !result.current.isFetching);

      expect(fetch).toHaveBeenCalledWith('GET', getUrl('recentActivity'));
      expect(result.current.data).toEqual(mockedRecentActivity);
    });

    it('Test trigger notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useRecentActivity(DEFAULT_PAGINATION), { wrapper });

      await suppressConsole(async () => {
        await waitFor(() => result.current.isFetching);
        await waitFor(() => !result.current.isFetching);
      });

      expect(UserNotification.error).toHaveBeenCalledWith(
        'Loading recent activity failed with status: Error: Error',
        'Could not load recent activity');
    });
  });
});
