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

import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { TableLayoutPreferences, TableLayoutPreferencesJSON } from 'components/common/EntityDataTable/types';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

export const tableLayoutQK = (entityId: string) => ['table-layout', entityId] as const;

const preferencesFromJSON = ({
  attributes,
  sort,
  per_page,
  custom_preferences,
  order,
}: TableLayoutPreferencesJSON): TableLayoutPreferences => ({
  attributes,
  sort: sort ? { attributeId: sort.field, direction: sort.order } : undefined,
  perPage: per_page,
  customPreferences: custom_preferences,
  order,
});

const emptyPrefs = (): TableLayoutPreferences<any> => ({
  attributes: [],
  sort: undefined,
  perPage: undefined as any, // keep shape; adapt if your type requires a number
  customPreferences: undefined,
  order: [],
});

const fetchUserLayoutPreferences = (entityId: string) =>
  fetch('GET', qualifyUrl(`/entitylists/preferences/${entityId}`))
    .then((res) => preferencesFromJSON(res ?? ({} as TableLayoutPreferencesJSON)))
    .catch((e) => {
      // Surface via defaultOnError to keep existing behavior
      throw e;
    });

const useUserLayoutPreferences = <T>(
  entityId: string,
): { data: TableLayoutPreferences<T>; isInitialLoading: boolean; refetch: () => void } => {
  const { data, isInitialLoading, refetch } = useQuery({
    queryKey: tableLayoutQK(entityId),
    queryFn: () =>
      defaultOnError(
        fetchUserLayoutPreferences(entityId),
        `Loading layout preferences for "${entityId}" overview failed with`,
      ),
    // Keep last successful data on screen during background transitions
    placeholderData: (prev) => prev ?? keepPreviousData,
    // Reduce surprise background refetches that could snap the UI
    staleTime: 60 * 60 * 1000, // 1 hour
    refetchOnWindowFocus: false,
    refetchOnMount: false,
    select: (prefs) => (prefs ?? emptyPrefs()) as TableLayoutPreferences<T>,
  });

  return {
    data: (data ?? emptyPrefs()) as TableLayoutPreferences<T>,
    isInitialLoading,
    refetch,
  };
};

export default useUserLayoutPreferences;
