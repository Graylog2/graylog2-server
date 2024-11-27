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
import { layoutPreferencesJSON, layoutPreferences } from 'fixtures/entityListLayoutPreferences';

import useUserLayoutPreferences from './useUserLayoutPreferences';

const createQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }) => (
  <QueryClientProvider client={createQueryClient()}>
    {children}
  </QueryClientProvider>
);

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

describe('useUserSearchFilterQuery hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should return layout preferences', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(layoutPreferencesJSON));
    const { result, waitFor } = renderHook(() => useUserLayoutPreferences('streams'), { wrapper });

    await waitFor(() => result.current.isInitialLoading);
    await waitFor(() => !result.current.isInitialLoading);

    expect(fetch).toHaveBeenCalledWith('GET', expect.stringContaining('/entitylists/preferences/streams'));
    expect(result.current.data).toEqual(layoutPreferences);
  });

  it('should trigger notification on error', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error!')));

    const { result, waitFor } = renderHook(() => useUserLayoutPreferences('streams'), { wrapper });

    await suppressConsole(async () => {
      await waitFor(() => result.current.isInitialLoading);
      await waitFor(() => !result.current.isInitialLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith('Loading layout preferences for "streams" overview failed with: Error: Error!', undefined);
  });
});
