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
import Immutable from 'immutable';

import type {
  TableLayoutPreferences,
  TableLayoutPreferencesJSON,
  TableLayoutDefaultFiltersJSON,
} from 'components/common/EntityDataTable/types';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import { TABLE_LAYOUT_DEFAULT_FILTERS_KEY_SPLITTER } from 'components/common/EntityDataTable/Constants';

const INITIAL_DATA = {};

const filtersFromJson = (filters: TableLayoutDefaultFiltersJSON): UrlQueryFilters =>
  filters.reduce<UrlQueryFilters>((queryFilters, jsonFilter) => {
    const [key, stringValues] = jsonFilter.split(TABLE_LAYOUT_DEFAULT_FILTERS_KEY_SPLITTER);
    if (queryFilters.get(key)) {
      return queryFilters.set(key, queryFilters[key].push(stringValues));
    }

    return queryFilters.set(key, [stringValues]);
  }, Immutable.OrderedMap({}));

const preferencesFromJSON = (preferences: TableLayoutPreferencesJSON): TableLayoutPreferences => {
  const { attributes, sort, per_page, slicing, custom_preferences, order, filters } = preferences;
  const hasSlicingPreference = Object.prototype.hasOwnProperty.call(preferences, 'slicing');

  const defaultFilters = filters ? filtersFromJson(filters) : undefined;

  const result: TableLayoutPreferences = {
    attributes,
    sort: sort ? { attributeId: sort.field, direction: sort.order } : undefined,
    perPage: per_page,
    customPreferences: custom_preferences,
    order,
    defaultFilters,
  };

  if (hasSlicingPreference) {
    result.slicing = slicing
      ? {
          sliceColumn: slicing.slice_column,
          sortBy: slicing.sort_by,
          order: slicing.order,
          ...(slicing.read_only !== undefined ? { readOnly: slicing.read_only } : {}),
        }
      : null;
  }

  return result;
};
const preferencesUrl = (entityId: string, layoutVariant?: string) => {
  const params = layoutVariant ? `?layout_variant=${encodeURIComponent(layoutVariant)}` : '';

  return qualifyUrl(`/entitylists/preferences/${entityId}${params}`);
};

const fetchUserLayoutPreferences = (entityId: string, layoutVariant?: string) =>
  fetch('GET', preferencesUrl(entityId, layoutVariant)).then((res) => preferencesFromJSON(res ?? {}));

const useUserLayoutPreferences = <T>(
  entityId: string,
  layoutVariant?: string,
): { data: TableLayoutPreferences<T>; isInitialLoading: boolean; refetch: () => void } => {
  const {
    data,
    isLoading: isInitialLoading,
    refetch,
  } = useQuery({
    queryKey: ['table-layout', entityId, layoutVariant],
    queryFn: () =>
      defaultOnError(
        fetchUserLayoutPreferences(entityId, layoutVariant),
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
