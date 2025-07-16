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
import { useMutation, useQuery } from '@tanstack/react-query';

import { defaultOnError } from 'util/conditional/onError';
import type { SearchParams } from 'stores/PaginationTypes';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import {
  fetchErrors,
  fetchPaginatedLookupTables,
  purgeLookupTableKey,
  purgeAllLookupTableKey,
  testLookupTableKey,
  deleteLookupTable,
  fetchPaginatedCaches,
  deleteCache,
  fetchPaginatedDataAdapters,
  deleteDataAdapter,
  fetchLookupPreview,
} from './api/lookupTablesAPI';

export const lookupTablesKeyFn = (searchParams: SearchParams) => ['lookup-tables', 'search', searchParams];
export function useFetchLookupTables() {
  return { fetchPaginatedLookupTables, lookupTablesKeyFn };
}

export function usePurgeLookupTableKey() {
  const { mutateAsync, isPending } = useMutation({
    mutationFn: purgeLookupTableKey,
    onSuccess: () => {
      UserNotification.success('Lookup table key purged successfully');
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    purgeLookupTableKey: mutateAsync,
    purgingLookupTableKey: isPending,
  };
}

export function usePurgeAllLookupTableKey() {
  const { mutateAsync, isPending } = useMutation({
    mutationFn: purgeAllLookupTableKey,
    onSuccess: () => {
      UserNotification.success('Lookup table purged successfully');
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    purgeAllLookupTableKey: mutateAsync,
    purgingAllLookupTableKey: isPending,
  };
}

export function useTestLookupTableKey() {
  const { mutateAsync, isPending } = useMutation({
    mutationFn: testLookupTableKey,
    onSuccess: () => {
      UserNotification.success('Lookup table purged successfully');
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    testLookupTableKey: mutateAsync,
    testingLookupTableKey: isPending,
  };
}
export function useFetchLookupPreview(idOrName: string, enabled: boolean = false, size: number = 5) {
  const { data, isLoading } = useQuery({
    queryKey: ['lookup-preview', idOrName, size, enabled],
    queryFn: () => defaultOnError(fetchLookupPreview(idOrName, size), 'Failed to fetch lookup preview'),
    retry: false,
    enabled,
  });

  return {
    lookupPreview: data ?? { results: [], total: 0, supported: true },
    loadingLookupPreview: isLoading,
  };
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
