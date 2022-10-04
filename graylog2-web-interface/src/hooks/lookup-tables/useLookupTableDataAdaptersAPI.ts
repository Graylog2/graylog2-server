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

import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import UserNotification from 'util/UserNotification';

import { fetchAll, fetchErrors } from './api/dataAdaptersAPI';
import type { LUTDataAdapterAPIResponseType, LUTErrorsAPIResponseType } from './api/types';

type GetAllDataAdaptersType = {
  page?: number,
  perPage?: number,
  query?: string,
};

export const useGetAllDataAdapters = ({ page, perPage, query }: GetAllDataAdaptersType = {}) => {
  const { data, isLoading, error } = useQuery<LUTDataAdapterAPIResponseType, Error>(
    ['all-data-adapters', page, perPage, query],
    () => fetchAll(page, perPage, query),
    {
      onError: () => UserNotification.error(error.message),
      retry: 2,
    },
  );

  const defaultData = {
    dataAdapters: [],
    pagination: {
      count: 0,
      total: 0,
      page: 1,
      perPage: 10,
      query: null,
    },
  };

  const { dataAdapters, pagination } = isLoading ? defaultData : {
    dataAdapters: data.data_adapters,
    pagination: {
      count: data.count,
      total: data.total,
      page: data.page,
      perPage: data.per_page,
      query: data.query,
    },
  };

  return {
    dataAdapters,
    pagination,
    loadingDataAdapters: isLoading,
  };
};

export const useGetDataAdapterErrors = (dataAdapters: LookupTableAdapter[] = []) => {
  const { data, isLoading, error } = useQuery<LUTErrorsAPIResponseType, Error>(
    ['errors-data-adapters', dataAdapters],
    () => fetchErrors(dataAdapters),
    {
      onError: () => UserNotification.error(error.message),
      retry: 1,
    },
  );

  return {
    dataAdapterErrors: data?.data_adapters || {},
    loadingDataAdaptersErrors: isLoading,
  };
};
