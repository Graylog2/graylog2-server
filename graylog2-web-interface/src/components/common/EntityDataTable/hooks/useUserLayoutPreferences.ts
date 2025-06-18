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

const INITIAL_DATA = {};

const preferencesFromJSON = ({
  displayed_attributes,
  sort,
  per_page,
  custom_preferences,
}: TableLayoutPreferencesJSON): TableLayoutPreferences => ({
  displayedAttributes: displayed_attributes,
  sort: sort ? { attributeId: sort.field, direction: sort.order } : undefined,
  perPage: per_page,
  customPreferences: custom_preferences,
});
const fetchUserLayoutPreferences = (entityId: string) =>
  fetch('GET', qualifyUrl(`/entitylists/preferences/${entityId}`)).then((res) => preferencesFromJSON(res ?? {}));

const useUserLayoutPreferences = <T>(
  entityId: string,
): { data: TableLayoutPreferences<T>; isInitialLoading: boolean; refetch: () => void } => {
  const { data, isInitialLoading, refetch } = useQuery({
    queryKey: ['table-layout', entityId],

    queryFn: () =>
      defaultOnError(
        fetchUserLayoutPreferences(entityId),
        `Loading layout preferences for "${entityId}" overview failed with`,
      ),

    placeholderData: keepPreviousData,

    // 1 hour
    staleTime: 60 * (60 * 1000),
  });

  return {
    data: data ?? INITIAL_DATA,
    isInitialLoading,
    refetch,
  };
};

export default useUserLayoutPreferences;
