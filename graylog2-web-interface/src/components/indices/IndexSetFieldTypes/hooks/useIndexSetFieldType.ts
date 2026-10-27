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

import { SystemIndexSetsTypes } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { IndexSetFieldTypesQueryData } from 'components/indices/IndexSetFieldTypes/types';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

export const keyFn = (indexSetId: string, searchParams: SearchParams) => [
  'indexSetFieldTypes',
  indexSetId,
  searchParams,
];

export const fetchIndexSetFieldTypes = async (
  indexSetId: string,
  searchParams: SearchParams,
): Promise<IndexSetFieldTypesQueryData> =>
  SystemIndexSetsTypes.indexSetFieldTypesList(
    searchParams.sort.attributeId as Parameters<typeof SystemIndexSetsTypes.indexSetFieldTypesList>[0],
    indexSetId,
    searchParams.query || undefined,
    FiltersForQueryParams(searchParams.filters),
    searchParams.page,
    searchParams.pageSize,
    searchParams.sort.direction,
  ).then(({ elements, total, attributes }) => ({
    list: elements.map((fieldType) => ({
      id: fieldType.field_name,
      fieldName: fieldType.field_name,
      type: fieldType.type,
      origin: fieldType.origin,
      isReserved: fieldType.is_reserved,
    })),
    pagination: { total },
    attributes,
  }));

const useIndexSetFieldTypes = (
  indexSetId: string,
  searchParams: SearchParams,
  { enabled },
): {
  data: IndexSetFieldTypesQueryData;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(indexSetId, searchParams),

    queryFn: () =>
      defaultOnError(
        fetchIndexSetFieldTypes(indexSetId, searchParams),
        'Loading index field types failed with status',
        'Could not load index field types',
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

export default useIndexSetFieldTypes;
