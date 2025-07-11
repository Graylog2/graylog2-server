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
import { useQuery, useMutation } from '@tanstack/react-query';

import type { SearchParams } from 'stores/PaginationTypes';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import {
  fetchErrors,
  fetchPaginatedLookupTables,
  deleteLookupTable,
  fetchPaginatedCaches,
  fetchCacheTypes,
  deleteCache,
  fetchPaginatedDataAdapters,
  deleteDataAdapter,
  fetchDataAdapterTypes,
  createCache,
  updateCache,
  validateCache,
} from './api/lookupTablesAPI';

export const lookupTablesKeyFn = (searchParams: SearchParams) => ['lookup-tables', 'search', searchParams];
export function useFetchLookupTables() {
  return { fetchPaginatedLookupTables, lookupTablesKeyFn };
}

export function useDeleteLookupTable() {
  const { refetch } = useTableFetchContext();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: deleteLookupTable,
    onSuccess: () => {
      UserNotification.success('Lookup table deleted successfully');
      refetch();
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    deleteLookupTable: mutateAsync,
    deletingLookupTable: isPending,
  };
}

export const cachesKeyFn = (searchParams: SearchParams) => ['caches', 'search', searchParams];
export function useFetchCaches() {
  return { fetchPaginatedCaches, cachesKeyFn };
}

export function useFetchCacheTypes() {
  const { data, isLoading } = useQuery({
    queryKey: ['cache-types'],
    queryFn: fetchCacheTypes,
  });

  return { fetchingCacheTypes: isLoading, types: data };
}

export function useValidateCache() {
  const { mutateAsync } = useMutation({
    mutationFn: validateCache,
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return { validateCache: mutateAsync };
}

export function useCreateCache() {
  const {
    mutateAsync,
    isPending: isLoading,
  } = useMutation({
    mutationFn: createCache,
    onSuccess: () => {
      UserNotification.success('Cache created successfully');
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    createCache: mutateAsync,
    creatingCache: isLoading,
  };
}

export function useUpdateCache() {
  const {
    mutateAsync,
    isPending: isLoading,
  } = useMutation({
    mutationFn: updateCache,
    onSuccess: () => {
      UserNotification.success('Cache updated successfully');
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    updateCache: mutateAsync,
    updatingCache: isLoading,
  };
}

export function useDeleteCache() {
  const { refetch } = useTableFetchContext();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: deleteCache,
    onSuccess: () => {
      UserNotification.success('Cache deleted successfully');
      refetch();
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    deleteCache: mutateAsync,
    deletingCache: isPending,
  };
}

export const dataAdaptersKeyFn = (searchParams: SearchParams) => ['lookup-tables', 'search', searchParams];
export function useFetchDataAdapters() {
  return { fetchPaginatedDataAdapters, dataAdaptersKeyFn };
}

export function useFetchDataAdapterTypes() {
  const { data, isLoading } = useQuery({
    queryKey: ['data-adapter-types'],
    queryFn: fetchDataAdapterTypes,
  });

  return { fetchingDataAdapterTypes: isLoading, types: data };
}

export function useDeleteDataAdapter() {
  const { refetch } = useTableFetchContext();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: deleteDataAdapter,
    onSuccess: () => {
      UserNotification.success('Data Adapter deleted successfully');
      refetch();
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    deleteDataAdapter: mutateAsync,
    deletingDataAdapter: isPending,
  };
}

export function useFetchErrors() {
  return { fetchErrors };
}
