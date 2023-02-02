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
import { renderHook, act } from '@testing-library/react-hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';

import useUserLayoutPreferences from './useUserLayoutPreferences';

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

const layoutPreferences = {
  displayedAttributes: ['title', 'description'],
  perPage: 50,
  sort: { attributeId: 'title', direction: 'asc' } as const,
};

const layoutPreferencesJSON = {
  displayed_attributes: ['title', 'description'],
  per_page: 50,
  sort: { field: 'title', order: 'asc' },
};

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

jest.mock('./useUserLayoutPreferences');

describe('useUserSearchFilterQuery hook', () => {
  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isLoading: false });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should update user layout preferences', async () => {
    const { result, waitFor } = renderHook(() => useUpdateUserLayoutPreferences('streams'), { wrapper });

    act(() => {
      result.current.mutate(layoutPreferences);
    });

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/entitylists/preferences/streams'), layoutPreferencesJSON));
  });

  it('should allow partial update of user layout preferences', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isLoading: false });
    const { result, waitFor } = renderHook(() => useUpdateUserLayoutPreferences('streams'), { wrapper });

    act(() => {
      result.current.mutate({ perPage: 100 });
    });

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/entitylists/preferences/streams'), {
      displayed_attributes: layoutPreferencesJSON.displayed_attributes,
      sort: layoutPreferencesJSON.sort,
      per_page: 100,
    }));
  });
});
