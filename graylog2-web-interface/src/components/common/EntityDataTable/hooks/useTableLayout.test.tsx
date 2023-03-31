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
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

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

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('./useUserLayoutPreferences');

describe('useUserSearchFilterQuery hook', () => {
  const defaultLayout = {
    defaultSort: { attributeId: 'description', direction: 'asc' } as const,
    defaultPageSize: 20,
    defaultDisplayedAttributes: ['title'],
  };

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isInitialLoading: false });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should provide user layout preferences', async () => {
    const { result } = renderHook(() => useTableLayout({
      entityTableId: 'streams',
      ...defaultLayout,
    }), { wrapper });

    expect(result.current.layoutConfig).toEqual({
      displayedAttributes: layoutPreferences.displayedAttributes,
      sort: layoutPreferences.sort,
      pageSize: layoutPreferences.perPage,
    });
  });

  it('should return defaults when there are no user layout preferences', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: undefined, isInitialLoading: false });

    const { result } = renderHook(() => useTableLayout({
      entityTableId: 'streams',
      ...defaultLayout,
    }), { wrapper });

    expect(result.current.layoutConfig).toEqual({
      displayedAttributes: defaultLayout.defaultDisplayedAttributes,
      sort: defaultLayout.defaultSort,
      pageSize: defaultLayout.defaultPageSize,
    });
  });

  it('should merge user preferences with defaults', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: { perPage: layoutPreferences.perPage }, isInitialLoading: false });
    const { result } = renderHook(() => useTableLayout({
      entityTableId: 'streams',
      ...defaultLayout,
    }), { wrapper });

    expect(result.current.layoutConfig).toEqual({
      displayedAttributes: defaultLayout.defaultDisplayedAttributes,
      sort: defaultLayout.defaultSort,
      pageSize: layoutPreferences.perPage,
    });
  });
});
