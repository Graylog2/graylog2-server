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

import type { SearchParams } from 'stores/PaginationTypes';
import { type Attribute } from 'stores/PaginationTypes';
import { defaultOnError } from 'util/conditional/onError';

export type PaginatedResponse<T, M = unknown> = {
  list: Array<T>,
  pagination: {
    total: number
  },
  attributes: Array<Attribute>,
  meta?: M
}

export type FetchOptions = {
  refetchInterval?: number,
};

const useFetchEntities = <T, M = unknown>({
  fetchKey,
  searchParams,
  fetchEntities,
  enabled,
  humanName,
  fetchOptions = {},
}: {
  fetchKey: Array<unknown>,
  searchParams: SearchParams,
  fetchEntities: (searchParams: SearchParams) => Promise<PaginatedResponse<T, M>>
  enabled: boolean,
  humanName: string
  fetchOptions?: FetchOptions,
}): {
  isInitialLoading: boolean,
  data: PaginatedResponse<T, M>,
  refetch: () => void,
} => {
  const { data, isInitialLoading, refetch } = useQuery(
    fetchKey,
    () => defaultOnError(fetchEntities(searchParams), `Fetching ${humanName} failed with status`, `Could not retrieve ${humanName}`),
    {
      enabled,
      keepPreviousData: true,
      ...fetchOptions,
    },
  );

  return ({
    data,
    isInitialLoading,
    refetch,
  });
};

export default useFetchEntities;
