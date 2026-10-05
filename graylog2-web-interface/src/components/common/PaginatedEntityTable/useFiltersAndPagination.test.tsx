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
import { OrderedMap } from 'immutable';
import { act, renderHook } from 'wrappedTestingLibrary/hooks';

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import type { LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';

import { useWithLocalState, useWithURLParams } from './useFiltersAndPagination';

let mockQueryParamValues: Record<string, unknown> = {};
let mockQueryParamsValues: Record<string, unknown> = {};

const mockSetQueryParam = jest.fn();
const mockSetQueryParams = jest.fn();

jest.mock('routing/QueryParams', () => {
  const { useCallback, useState } = jest.requireActual('react');

  return {
    ArrayParam: {},
    NumberParam: {},
    StringParam: {},
    useQueryParam: jest.fn((param: string) => {
      const [value, setValue] = useState(mockQueryParamValues[param]);

      const setter = useCallback(
        (newValue: unknown) => {
          mockSetQueryParam(param, newValue);
          mockQueryParamValues = {
            ...mockQueryParamValues,
            [param]: newValue,
          };
          setValue(newValue);
        },
        [param],
      );

      return [value, setter];
    }),
    useQueryParams: jest.fn(() => {
      const [value, setValue] = useState(mockQueryParamsValues);

      const setter = useCallback((newValue: Record<string, unknown>) => {
        mockSetQueryParams(newValue);
        setValue((currentValue: Record<string, unknown>) => {
          const nextValue = {
            ...currentValue,
            ...newValue,
          };
          mockQueryParamsValues = nextValue;

          return nextValue;
        });
      }, []);

      return [value, setter];
    }),
  };
});

const defaultFilters: UrlQueryFilters = OrderedMap({
  priority: ['critical', 'high'],
  timestamp: ['relative@2592000'],
});

const layoutConfig = {
  attributes: undefined,
  order: undefined,
  pageSize: 20,
  slicing: {
    sliceColumn: 'owner',
    sortBy: 'risk_score',
    order: 'desc',
  },
  sort: {
    attributeId: 'timestamp',
    direction: 'desc',
  },
} as LayoutConfig;

const filtersAsObject = (filters?: UrlQueryFilters) => filters?.toJS();

describe('useFiltersAndPagination', () => {
  beforeEach(() => {
    mockQueryParamValues = {};
    mockQueryParamsValues = {};
    mockSetQueryParam.mockClear();
    mockSetQueryParams.mockClear();
  });

  describe('useWithURLParams', () => {
    it('resets URL filters, slice, and page while preserving the query', () => {
      mockQueryParamValues = {
        filters: ['status=OPEN'],
        query: 'message:error',
        slice: 'admin',
      };
      mockQueryParamsValues = { page: 5 };

      const { result } = renderHook(() => useWithURLParams(layoutConfig, defaultFilters));

      expect(filtersAsObject(result.current.searchParams.filters)).toEqual({ status: ['OPEN'] });
      expect(result.current.searchParams.query).toEqual('message:error');
      expect(result.current.searchParams.slice).toEqual('admin');
      expect(result.current.searchParams.page).toEqual(5);

      act(() => {
        result.current.resetFilters();
      });

      expect(filtersAsObject(result.current.searchParams.filters)).toEqual(defaultFilters.toJS());
      expect(result.current.searchParams.query).toEqual('message:error');
      expect(result.current.searchParams.slice).toBeUndefined();
      expect(result.current.searchParams.page).toEqual(1);
      expect(mockSetQueryParam).toHaveBeenCalledWith('filters', []);
      expect(mockSetQueryParam).toHaveBeenCalledWith('slice', undefined);
      expect(mockSetQueryParams).toHaveBeenCalledWith({ page: 1, pageSize: undefined });
    });

    it('reapplies default filters after user filters were changed', () => {
      const { result } = renderHook(() => useWithURLParams(layoutConfig, defaultFilters));

      expect(filtersAsObject(result.current.searchParams.filters)).toEqual(defaultFilters.toJS());

      act(() => {
        result.current.onChangeFilters(OrderedMap({ status: ['OPEN'] }));
      });

      expect(filtersAsObject(result.current.searchParams.filters)).toEqual({ status: ['OPEN'] });

      act(() => {
        result.current.resetFilters();
      });

      expect(filtersAsObject(result.current.searchParams.filters)).toEqual(defaultFilters.toJS());
      expect(result.current.searchParams.page).toEqual(1);
    });
  });

  describe('useWithLocalState', () => {
    it('resets local filters, slice, page, and query to the default state', () => {
      const { result } = renderHook(() => useWithLocalState(layoutConfig, defaultFilters));

      act(() => {
        result.current.setQuery('message:error');
        result.current.onChangeFilters(OrderedMap({ status: ['OPEN'] }));
        result.current.onChangeSlicingFilter('admin');
        result.current.paginationState.setPagination({ page: 5, pageSize: 100 });
      });

      expect(result.current.searchParams.query).toEqual('message:error');
      expect(filtersAsObject(result.current.searchParams.filters)).toEqual({ status: ['OPEN'] });
      expect(result.current.searchParams.slice).toEqual('admin');
      expect(result.current.searchParams.page).toEqual(5);

      act(() => {
        result.current.resetFilters();
      });

      expect(result.current.searchParams.query).toEqual('');
      expect(filtersAsObject(result.current.searchParams.filters)).toEqual(defaultFilters.toJS());
      expect(result.current.searchParams.slice).toBeUndefined();
      expect(result.current.searchParams.page).toEqual(1);
      expect(result.current.searchParams.pageSize).toEqual(layoutConfig.pageSize);
      expect(result.current.searchParams.sliceCol).toEqual(layoutConfig.slicing?.sliceColumn);
      expect(result.current.searchParams.sort).toEqual(layoutConfig.sort);
    });
  });
});
