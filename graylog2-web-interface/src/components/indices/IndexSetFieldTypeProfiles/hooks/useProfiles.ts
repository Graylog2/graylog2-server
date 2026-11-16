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

import { SystemIndexSetsFieldTypeProfiles } from '@graylog/server-api';

import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFieldTypeProfiles/types';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

const usedInAttribute: Attribute = {
  id: 'index_set_ids',
  searchable: false,
  sortable: false,
  title: 'Used in',
  type: 'STRING',
};

export const keyFn = (searchParams: SearchParams) => ['indexSetFieldTypeProfiles', searchParams];

export const fetchIndexSetFieldTypeProfiles = async (searchParams: SearchParams) =>
  SystemIndexSetsFieldTypeProfiles.getPage(
    searchParams.sort.attributeId as Parameters<typeof SystemIndexSetsFieldTypeProfiles.getPage>[0],
    searchParams.page,
    searchParams.pageSize,
    searchParams.query || undefined,
    FiltersForQueryParams(searchParams.filters),
    searchParams.sort.direction,
  ).then(({ elements, total, attributes }) => ({
    list: elements.map((profile) => ({
      id: profile.id,
      name: profile.name,
      description: profile.description,
      customFieldMappings: profile.custom_field_mappings,
      indexSetIds: profile.index_set_ids,
    })),
    pagination: { total },
    attributes: [...attributes, usedInAttribute],
  }));

const useProfiles = (
  searchParams: SearchParams,
  { enabled },
): {
  data: {
    list: Readonly<Array<IndexSetFieldTypeProfile>>;
    pagination: { total: number };
    attributes: Array<Attribute>;
  };
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError(
        fetchIndexSetFieldTypeProfiles(searchParams),
        'Loading index field type profiles failed with status',
        'Could not load index field type profiles',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  };
};

export default useProfiles;
