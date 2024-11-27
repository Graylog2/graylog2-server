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

import type { TableLayoutPreferences, TableLayoutPreferencesJSON } from 'components/common/EntityDataTable/types';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {};

const preferencesFromJSON = ({
  displayed_attributes,
  sort,
  per_page,
}: TableLayoutPreferencesJSON): TableLayoutPreferences => ({
  displayedAttributes: displayed_attributes,
  sort: sort ? { attributeId: sort.field, direction: sort.order } : undefined,
  perPage: per_page,
});
const fetchUserLayoutPreferences = (entityId: string) => fetch(
  'GET',
  qualifyUrl(`/entitylists/preferences/${entityId}`),
).then((res) => preferencesFromJSON(res ?? {}));

const useUserLayoutPreferences = (entityId: string): { data: TableLayoutPreferences, isInitialLoading: boolean } => {
  const { data, isInitialLoading } = useQuery(
    ['table-layout', entityId],
    () => defaultOnError(fetchUserLayoutPreferences(entityId), `Loading layout preferences for "${entityId}" overview failed with`),
    {
      keepPreviousData: true,
      staleTime: 60 * (60 * 1000), // 1 hour
    });

  return {
    data: data ?? INITIAL_DATA,
    isInitialLoading,
  };
};

export default useUserLayoutPreferences;
