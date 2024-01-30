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

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { IndexSetFieldType, IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

const fetchIndexSetFieldTypes = async (indexSetId: string, searchParams: SearchParams) => {
  const indexSetFieldTypeUrl = qualifyUrl(`/system/indices/index_sets/types/${indexSetId}`);
  const url = PaginationURL(
    indexSetFieldTypeUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { filters: FiltersForQueryParams(searchParams.filters), sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  return fetch('GET', url).then(
    ({ elements, total, attributes }) => ({
      list: elements.map((fieldType: IndexSetFieldTypeJson) => ({
        id: fieldType.field_name,
        fieldName: fieldType.field_name,
        type: fieldType.type,
        origin: fieldType.origin,
        isReserved: fieldType.is_reserved,
      })),
      pagination: { total },
      attributes,
    }));
};

const useIndexSetFieldTypes = (indexSetId: string, searchParams: SearchParams, { enabled }): {
  data: {
    list: Readonly<Array<IndexSetFieldType>>,
    pagination: { total: number },
    attributes: Array<Attribute>
  },
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['indexSetFieldTypes', searchParams],
    () => fetchIndexSetFieldTypes(indexSetId, searchParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field types failed with status: ${errorThrown}`,
          'Could not load index field types');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  });
};

export default useIndexSetFieldTypes;
