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

import UserNotification from 'preflight/util/UserNotification';
import type { SearchParams } from 'stores/PaginationTypes';
import { type Attribute } from 'stores/PaginationTypes';

export type PaginatedResponse<T> = {
  list: Array<T>,
  pagination: {
    total: number
  },
  attributes: Array<Attribute>,
}

const useFetchEntities = <T>({
  fetchKey,
  searchParams,
  fetchData,
  enabled,
  humanName,
}: {
  fetchKey: Array<unknown>,
  searchParams: SearchParams,
  fetchData: (searchParams: SearchParams) => Promise<PaginatedResponse<T>>
  enabled: boolean,
  humanName: string
}): {
  isInitialLoading: boolean,
  data: PaginatedResponse<T>,
  refetch: () => void,
} => {
  const { data, isInitialLoading, refetch } = useQuery(
    fetchKey,
    () => fetchData(searchParams),
    {
      enabled,
      onError: (error) => {
        UserNotification.error(`Fetching ${humanName} failed with status: ${error}`, `Could not retrieve ${humanName}`);
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    isInitialLoading,
    refetch,
  });
};

export default useFetchEntities;
