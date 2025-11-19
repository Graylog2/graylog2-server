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
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { defaultOnError } from 'util/conditional/onError';
import type { SearchParams } from 'stores/PaginationTypes';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import {
  fetchErrors,
  fetchPaginatedLookupTables,
  fetchLookupTable,
  createLookupTable,
  updateLookupTable,
  purgeLookupTableKey,
  purgeAllLookupTableKey,
  testLookupTableKey,
  deleteLookupTable,
  fetchPaginatedCaches,
  fetchCache,
  fetchCacheTypes,
  deleteCache,
  fetchPaginatedDataAdapters,
  deleteDataAdapter,
  fetchLookupPreview,
  fetchDataAdapter,
  fetchDataAdapterTypes,
  createCache,
  updateCache,
  validateCache,
  createDataAdapter,
  updateDataAdapter,
  validateDataAdapter,
} from './api/lookupTablesAPI';

export const lookupTablesKeyFn = (searchParams: SearchParams) => ['lookup-tables', 'search', searchParams];
export function useFetchLookupTables() {
  return { fetchPaginatedLookupTables, lookupTablesKeyFn };
}

export function useFetchLookupTable(idOrName: string) {
  const { data, isLoading } = useQuery({
    queryKey: ['lookup-table-details', idOrName],
    queryFn: () => defaultOnError(fetchLookupTable(idOrName), 'Failed to fetch lookup table'),
    retry: false,
    enabled: !!idOrName,
  });

  const { lookup_tables } = data || { lookup_tables: [] };

  return {
    lookupTable: lookup_tables?.[0],
    loadingLookupTable: isLoading,
  };
}

export function useCreateLookupTable() {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: createLookupTable,
    onSuccess: () => {
      UserNotification.success('Lookup Table created successfully');
      queryClient.invalidateQueries({
        queryKey: ['lookup-tables'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['lookup-table-details'],
        refetchType: 'active',
      });
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    createLookupTable: mutateAsync,
    creatingLookupTable: isLoading,
  };
}

export function useUpdateLookupTable() {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: updateLookupTable,
    onSuccess: () => {
      UserNotification.success('Lookup Table updated successfully');
      queryClient.invalidateQueries({
        queryKey: ['lookup-tables'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['lookup-table-details'],
        refetchType: 'active',
      });
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    updateLookupTable: mutateAsync,
    updatingLookupTable: isLoading,
  };
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

export function useFetchAllCaches() {
  const { data, isLoading } = useQuery({
    queryKey: ['all-caches'],
    queryFn: () =>
      defaultOnError(
        fetchPaginatedCaches({
          page: 1,
          pageSize: 10000,
          query: null,
          sort: { attributeId: 'name', direction: 'asc' },
        }),
        'Failed to fetch all caches',
      ),
    retry: false,
  });

  return {
    allCaches: data?.list || [],
    loadingAllCaches: isLoading,
  };
}

export function useFetchCache(idOrName: string) {
  const { data, isLoading } = useQuery({
    queryKey: ['cache-details', idOrName],
    queryFn: () => defaultOnError(fetchCache(idOrName), 'Failed to fetch cache'),
    retry: false,
    enabled: !!idOrName,
  });

  return {
    cache: data,
    loadingCache: isLoading,
  };
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
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: createCache,
    onSuccess: () => {
      UserNotification.success('Cache created successfully');
      queryClient.invalidateQueries({
        queryKey: ['caches'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['all-caches'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['cache-details'],
        refetchType: 'active',
      });
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    createCache: mutateAsync,
    creatingCache: isLoading,
  };
}

export function useUpdateCache() {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: updateCache,
    onSuccess: () => {
      UserNotification.success('Cache updated successfully');
      queryClient.invalidateQueries({
        queryKey: ['caches'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['all-caches'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['cache-details'],
        refetchType: 'active',
      });
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

export const dataAdaptersKeyFn = (searchParams: SearchParams) => ['adapters', 'search', searchParams];
export function useFetchDataAdapters() {
  return { fetchPaginatedDataAdapters, dataAdaptersKeyFn };
}

export function useFetchAllDataAdapters() {
  const { data, isLoading } = useQuery({
    queryKey: ['all-data-adapters'],
    queryFn: () =>
      defaultOnError(
        fetchPaginatedDataAdapters({
          page: 1,
          pageSize: 10000,
          query: null,
          sort: { attributeId: 'name', direction: 'asc' },
        }),
        'Failed to fetch all data adapters',
      ),
    retry: false,
  });

  return {
    allDataAdapters: data?.list || [],
    loadingAllDataAdapters: isLoading,
  };
}

export function useFetchDataAdapterTypes() {
  const { data, isLoading } = useQuery({
    queryKey: ['data-adapter-types'],
    queryFn: fetchDataAdapterTypes,
  });

  return { fetchingDataAdapterTypes: isLoading, types: data };
}

export function useFetchDataAdapter(idOrName: string) {
  const { data, isFetching } = useQuery({
    queryKey: ['data-adapter-details', idOrName],
    queryFn: () => defaultOnError(fetchDataAdapter(idOrName), 'Failed to fetch data adapter'),
    retry: false,
    enabled: !!idOrName,
  });

  return {
    dataAdapter: data,
    loadingDataAdapter: isFetching,
  };
}

export function useCreateAdapter() {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: createDataAdapter,
    onSuccess: () => {
      UserNotification.success('Data Adapter created successfully');
      queryClient.invalidateQueries({
        queryKey: ['adapters'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['all-data-adapters'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['data-adapter-details'],
        refetchType: 'active',
      });
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    createAdapter: mutateAsync,
    creatingAdapter: isLoading,
  };
}

export function useUpdateAdapter() {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending: isLoading } = useMutation({
    mutationFn: updateDataAdapter,
    onSuccess: () => {
      UserNotification.success('Data Adapter updated successfully');
      queryClient.invalidateQueries({
        queryKey: ['adapters'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['all-data-adapters'],
        refetchType: 'active',
      });
      queryClient.invalidateQueries({
        queryKey: ['data-adapter-details'],
        refetchType: 'active',
      });
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    updateAdapter: mutateAsync,
    updatingAdapter: isLoading,
  };
}

export function useValidateDataAdapter() {
  const { mutateAsync } = useMutation({
    mutationFn: validateDataAdapter,
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return { validateDataAdapter: mutateAsync };
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
