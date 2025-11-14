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

export const useWithURLParams = (layoutConfig: LayoutConfig) => {
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setUrlQuery] = useQueryParam('query', StringParam);
  const urlPagination = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);

  const fetchOptions: SearchParams = useMemo(
    () => ({
      query,
      page: urlPagination.page,
      pageSize: layoutConfig.pageSize,
      sort: layoutConfig.sort,
      filters: urlQueryFilters,
    }),
    [layoutConfig.pageSize, layoutConfig.sort, urlPagination.page, query, urlQueryFilters],
  );

  const onChangeFilters = useCallback(
    (newUrlQueryFilters: UrlQueryFilters) => {
      urlPagination.resetPage();
      setUrlQueryFilters(newUrlQueryFilters);
    },
    [urlPagination, setUrlQueryFilters],
  );

  return {
    fetchOptions,
    setQuery: setUrlQuery,
    onChangeFilters,
    paginationState: urlPagination,
  };
};

export const useWithLocalState = (layoutConfig: LayoutConfig) => {
  const [transientFetchOptions, setTransientFetchOptions] = useState<any>({
    query: '',
    page: DEFAULT_PAGE,
    filters: OrderedMap<string, Array<string>>(),
  });

  const setPagination = useCallback(
    ({ page, pageSize }: { page?: number; pageSize?: number }) => {
      setTransientFetchOptions({
        ...transientFetchOptions,
        page,
        pageSize,
      });
    },
    [transientFetchOptions],
  );

  const resetPage = useCallback(() => {
    setTransientFetchOptions({
      ...transientFetchOptions,
      page: DEFAULT_PAGE,
      sort: layoutConfig.sort,
    });
  }, [transientFetchOptions, layoutConfig.sort]);

  const onChangeFilters = useCallback(
    (newFilters: UrlQueryFilters) => {
      setTransientFetchOptions({
        ...transientFetchOptions,
        page: DEFAULT_PAGE,
        filters: newFilters,
      });
    },
    [transientFetchOptions],
  );

  const setQuery = useCallback(
    (newQuery: string) => {
      setTransientFetchOptions({
        ...transientFetchOptions,
        query: newQuery,
        page: DEFAULT_PAGE,
      });
    },
    [transientFetchOptions],
  );

  const fetchOptions: SearchParams = useMemo(
    () => ({
      ...transientFetchOptions,
      pageSize: layoutConfig.pageSize,
      sort: layoutConfig.sort,
    }),
    [transientFetchOptions, layoutConfig.pageSize, layoutConfig.sort],
  );

  return useMemo(
    () => ({
      fetchOptions,
      setQuery,
      onChangeFilters,
      paginationState: {
        page: fetchOptions.page,
        pageSize: fetchOptions.pageSize,
        resetPage,
        setPagination,
      },
    }),
    [fetchOptions, onChangeFilters, resetPage, setPagination, setQuery],
  );
};
