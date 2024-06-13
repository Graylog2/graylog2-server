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
import { useQuery } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';

const INITIAL_DATA = {
  pagination: { total: 0 },
  elements: [],
  attributes: [],
};

export const KEY_PREFIX = ['streams', 'overview'];
export const keyFn = (searchParams: SearchParams) => [...KEY_PREFIX, searchParams];

export const fetchStreams = (searchParams: SearchParams) => StreamsStore.searchPaginated(
  searchParams.page,
  searchParams.pageSize,
  searchParams.query,
  {
    sort: searchParams?.sort.attributeId,
    order: searchParams?.sort.direction,
    filters: FiltersForQueryParams(searchParams.filters),
  },
);

type Options = {
  enabled: boolean,
}

const useStreams = (searchParams: SearchParams, { enabled }: Options = { enabled: true }): {
  data: {
    list: Array<Stream>,
    pagination: { total: number }
    attributes: Array<Attribute>
  },
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    keyFn(searchParams),
    () => fetchStreams(searchParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    refetch,
    isInitialLoading,
  });
};

export default useStreams;
