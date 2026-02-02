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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';
import { OrderedMap } from 'immutable';
import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import DefaultQueryParamProvider from 'routing/DefaultQueryParamProvider';
import type { LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';

import { useWithURLParams, useWithLocalState } from './useFiltersAndPagination';

const mockLayoutConfig: LayoutConfig = {
  pageSize: 20,
  sort: { attributeId: 'timestamp', direction: 'desc' },
  attributes: {},
  order: [],
};

describe('useFiltersAndPagination', () => {
  describe('useWithLocalState', () => {
    it('should initialize with empty filters when no defaults provided', () => {
      const { result } = renderHook(() => useWithLocalState(mockLayoutConfig));

      expect(result.current.fetchOptions.filters).toEqual(OrderedMap());
    });

    it('should initialize with default filters when provided', () => {
      const defaultFilters = OrderedMap({ timestamp: ['2025-01-01T00:00:00.000+00:00><'] });
      const { result } = renderHook(() => useWithLocalState(mockLayoutConfig, defaultFilters));

      expect(result.current.fetchOptions.filters).toEqual(defaultFilters);
    });

    it('should allow changing filters after initialization with defaults', () => {
      const defaultFilters = OrderedMap({ timestamp: ['2025-01-01T00:00:00.000+00:00><'] });
      const { result } = renderHook(() => useWithLocalState(mockLayoutConfig, defaultFilters));

      const newFilters = OrderedMap({ priority: ['3'] });

      act(() => {
        result.current.onChangeFilters(newFilters);
      });

      expect(result.current.fetchOptions.filters).toEqual(newFilters);
    });
  });

  describe('useWithURLParams', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter>
        <DefaultQueryParamProvider>{children}</DefaultQueryParamProvider>
      </MemoryRouter>
    );

    it('should initialize with empty filters when no defaults and no URL params', () => {
      const { result } = renderHook(() => useWithURLParams(mockLayoutConfig), { wrapper });

      expect(result.current.fetchOptions.filters).toEqual(OrderedMap());
    });

    it('should apply default filters when URL has no filter params', () => {
      const defaultFilters = OrderedMap({ timestamp: ['2025-01-01T00:00:00.000+00:00><'] });
      const { result } = renderHook(() => useWithURLParams(mockLayoutConfig, defaultFilters), { wrapper });

      expect(result.current.fetchOptions.filters).toEqual(defaultFilters);
    });

    it('should prefer URL params over default filters when URL has filters', () => {
      const defaultFilters = OrderedMap({ timestamp: ['2025-01-01T00:00:00.000+00:00><'] });
      const urlFilters = OrderedMap({ priority: ['3'] });

      const WrapperWithFilters = ({ children }: { children: React.ReactNode }) => (
        <MemoryRouter initialEntries={['/?filters=priority%3D3']}>
          <DefaultQueryParamProvider>{children}</DefaultQueryParamProvider>
        </MemoryRouter>
      );

      const { result } = renderHook(() => useWithURLParams(mockLayoutConfig, defaultFilters), {
        wrapper: WrapperWithFilters,
      });

      expect(result.current.fetchOptions.filters).toEqual(urlFilters);
    });

    it('should allow changing filters after initialization with defaults', () => {
      const defaultFilters = OrderedMap({ timestamp: ['2025-01-01T00:00:00.000+00:00><'] });
      const { result } = renderHook(() => useWithURLParams(mockLayoutConfig, defaultFilters), { wrapper });

      const newFilters = OrderedMap({ priority: ['3'] });

      act(() => {
        result.current.onChangeFilters(newFilters);
      });

      expect(result.current.fetchOptions.filters).toEqual(newFilters);
    });
  });
});
