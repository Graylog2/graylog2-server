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
import type {
  IndexSetFieldTypeProfileJson,
  IndexSetFieldTypeProfile,
} from 'components/indices/IndexSetFieldTypeProfiles/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

const fetchIndexSetFieldTypeProfiles = async (searchParams: SearchParams) => {
  const indexSetFieldTypeUrl = qualifyUrl('/system/indices/index_sets/profiles/paginated');
  const url = PaginationURL(
    indexSetFieldTypeUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { filters: FiltersForQueryParams(searchParams.filters), sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  return fetch('GET', url).then(
    ({ elements, total, attributes }) => ({
      list: elements.map((profile: IndexSetFieldTypeProfileJson) => ({
        id: profile.id,
        name: profile.name,
        description: profile.description,
        customFieldMappings: profile.custom_field_mappings,
        indexSetIds: profile.index_set_ids,
      })),
      pagination: { total },
      attributes: [...attributes, {
        id: 'index_set_ids',
        searchable: false,
        sortable: false,
        title: 'Used in',
        type: 'STRING',
      }],
    }));
};

const useProfiles = (searchParams: SearchParams, { enabled }): {
  data: {
    list: Readonly<Array<IndexSetFieldTypeProfile>>,
    pagination: { total: number },
    attributes: Array<Attribute>
  },
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['indexSetFieldTypeProfiles', searchParams],
    () => fetchIndexSetFieldTypeProfiles(searchParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field type profiles failed with status: ${errorThrown}`,
          'Could not load index field type profiles');
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

export default useProfiles;
