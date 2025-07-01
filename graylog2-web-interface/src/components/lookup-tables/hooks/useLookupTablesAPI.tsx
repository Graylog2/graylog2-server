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
import { useMutation } from '@tanstack/react-query';

import type { SearchParams } from 'stores/PaginationTypes';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import { fetchPaginatedLookupTables, deleteLookupTable, fetchErrors } from './api/lookupTablesAPI';

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

export function useFetchErrors() {
  return { fetchErrors };
}
