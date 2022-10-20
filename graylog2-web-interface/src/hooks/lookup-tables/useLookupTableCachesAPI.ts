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

import { fetchAll } from './api/cachesAPI';
import type { LUTCacheAPIResponseType } from './api/types';

type GetAllCachesType = {
  page?: number,
  perPage?: number,
  query?: string,
};

// eslint-disable-next-line import/prefer-default-export
export const useGetAllCaches = ({ page, perPage, query }: GetAllCachesType = {}) => {
  const { data, isLoading, error } = useQuery<LUTCacheAPIResponseType, Error>(
    ['all-caches', page, perPage, query],
    () => fetchAll(page, perPage, query),
    {
      onError: () => UserNotification.error(error.message),
      retry: 2,
    },
  );

  const defaultData = {
    caches: [],
    pagination: {
      count: 0,
      total: 0,
      page: 1,
      perPage: 10,
      query: null,
    },
  };

  const { caches, pagination } = isLoading ? defaultData : {
    caches: data.caches,
    pagination: {
      count: data.count,
      total: data.total,
      page: data.page,
      perPage: data.per_page,
      query: data.query,
    },
  };

  return {
    caches,
    pagination,
    loadingCaches: isLoading,
  };
};
