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
import { useCallback, useMemo, useState } from 'react';
import { OrderedMap } from 'immutable';

import { useQueryParam, StringParam } from 'routing/QueryParams';
import usePaginationQueryParameter, { DEFAULT_PAGE } from 'hooks/usePaginationQueryParameter';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { SearchParams } from 'stores/PaginationTypes';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import type { LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { TableFilterContextValue } from 'components/common/PaginatedEntityTable/TableFilterContext';

export const useWithURLParams = (
  layoutConfig: LayoutConfig,
  defaultFilters?: UrlQueryFilters,
): TableFilterContextValue => {
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setUrlQuery] = useQueryParam('query', StringParam);
  const [slice, setSlice] = useQueryParam('slice', StringParam);
  const urlPagination = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const [hasUserModifiedFilters, setHasUserModifiedFilters] = useState(false);

  const effectiveFilters = useMemo(() => {
    if (!hasUserModifiedFilters && urlQueryFilters.isEmpty() && defaultFilters && !defaultFilters.isEmpty()) {
      return defaultFilters;
    }

    return urlQueryFilters;
  }, [hasUserModifiedFilters, urlQueryFilters, defaultFilters]);

  const searchParams: SearchParams = useMemo(
    () => ({
      query,
      slice,
      sliceCol: layoutConfig.slicing?.sliceColumn,
      page: urlPagination.page,
      pageSize: layoutConfig.pageSize,
      sort: layoutConfig.sort,
      filters: effectiveFilters,
    }),
    [
      query,
      slice,
      layoutConfig.slicing?.sliceColumn,
      urlPagination.page,
      layoutConfig.pageSize,
      layoutConfig.sort,
      effectiveFilters,
    ],
  );

  const onChangeFilters = useCallback(
    (newUrlQueryFilters: UrlQueryFilters) => {
      setHasUserModifiedFilters(true);
      urlPagination.resetPage();
      setUrlQueryFilters(newUrlQueryFilters);
    },
    [urlPagination, setUrlQueryFilters],
  );

  const onChangeSlicingFilter = useCallback(
    (newSlice?: string) => {
      urlPagination.resetPage();
      setSlice(newSlice);
    },
    [setSlice, urlPagination],
  );

  const resetFilters = useCallback(() => {
    setHasUserModifiedFilters(false);
    setUrlQueryFilters(OrderedMap({}));
    setSlice(undefined);
    urlPagination.resetPage();
  }, [setSlice, setUrlQueryFilters, urlPagination]);

  return useMemo(
    () => ({
      searchParams,
      setQuery: setUrlQuery,
      onChangeFilters,
      onChangeSlicingFilter,
      paginationState: urlPagination,
      resetFilters,
    }),
    [searchParams, onChangeFilters, onChangeSlicingFilter, resetFilters, setUrlQuery, urlPagination],
  );
};

export const useWithLocalState = (
  layoutConfig: LayoutConfig,
  defaultFilters?: UrlQueryFilters,
): TableFilterContextValue => {
  const defaultState = useMemo(
    () => ({
      query: '',
      page: DEFAULT_PAGE,
      filters: defaultFilters ?? OrderedMap<string, Array<string>>(),
      slice: undefined,
    }),
    [defaultFilters],
  );

  const [transientFetchOptions, setTransientFetchOptions] = useState<any>(defaultState);

  const setPagination = useCallback(({ page, pageSize }: { page?: number; pageSize?: number }) => {
    setTransientFetchOptions((cur) => ({
      ...cur,
      page,
      pageSize,
    }));
  }, []);

  const resetPage = useCallback(() => {
    setTransientFetchOptions((cur) => ({
      ...cur,
      page: DEFAULT_PAGE,
      sort: layoutConfig.sort,
    }));
  }, [layoutConfig.sort]);

  const onChangeFilters = useCallback((newFilters: UrlQueryFilters) => {
    setTransientFetchOptions((cur) => ({
      ...cur,
      page: DEFAULT_PAGE,
      filters: newFilters,
    }));
  }, []);

  const onChangeSlicingFilter = useCallback((slice?: string | undefined) => {
    setTransientFetchOptions((cur) => ({
      ...cur,
      page: DEFAULT_PAGE,
      slice,
    }));
  }, []);

  const setQuery = useCallback((newQuery: string) => {
    setTransientFetchOptions((cur) => ({
      ...cur,
      query: newQuery,
      page: DEFAULT_PAGE,
    }));
  }, []);

  const searchParams: SearchParams = useMemo(
    () => ({
      ...transientFetchOptions,
      sliceCol: layoutConfig.slicing?.sliceColumn,
      pageSize: layoutConfig.pageSize,
      sort: layoutConfig.sort,
    }),
    [transientFetchOptions, layoutConfig.pageSize, layoutConfig.slicing?.sliceColumn, layoutConfig.sort],
  );

  const resetFilters = useCallback(() => {
    setTransientFetchOptions(defaultState);
  }, [defaultState]);

  return useMemo(
    () => ({
      searchParams,
      setQuery,
      onChangeFilters,
      onChangeSlicingFilter,
      paginationState: {
        page: searchParams.page,
        pageSize: searchParams.pageSize,
        resetPage,
        setPagination,
      },
      resetFilters,
    }),
    [searchParams, onChangeFilters, onChangeSlicingFilter, resetFilters, resetPage, setPagination, setQuery],
  );
};
