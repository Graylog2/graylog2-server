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
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import type { FieldTypeUsage, PaginatedFieldTypeUsagesResponse } from 'views/logic/fieldactions/ChangeFieldType/types';
import fetch from 'logic/rest/FetchProvider';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type Options = {
  enabled: boolean,
}

const fieldTypeUsagesUrl = qualifyUrl('/system/indices/index_sets/types');

const fetchFieldTypeUsages = async ({ field, streams }: { field: string, streams: Array<string>}, searchParams: SearchParams) => {
  const url = PaginationURL(
    fieldTypeUsagesUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  return fetch<PaginatedFieldTypeUsagesResponse>('POST', qualifyUrl(url), { field, streams: streams.length ? streams : undefined }).then(
    ({ elements, total, count, page, per_page: perPage, attributes }) => ({
      list: elements
        .map(({
          stream_titles,
          index_set_title,
          index_set_id,
          types,
        }) => ({
          streamTitles: stream_titles,
          types,
          id: index_set_id,
          indexSetTitle: index_set_title,
        })),
      pagination: { total, count, page, perPage },
      attributes,
    }),
  );
};

const useFieldTypeUsages = ({ streams, field }: { streams: Array<string>, field: string }, searchParams: SearchParams, { enabled }: Options = { enabled: true }): {
  data: {
    list: Readonly<Array<FieldTypeUsage>>,
    pagination: { total: number },
    attributes: Array<Attribute>
  },
  refetch: () => void,
  isInitialLoading: boolean,
  isFirsLoaded: boolean,
} => {
  const { data, refetch, isInitialLoading, isLoading } = useQuery(
    ['fieldTypeUsages', field, searchParams],
    () => fetchFieldTypeUsages({ streams, field }, searchParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading ${field} types failed with status: ${errorThrown}`,
          'Could not load field types');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    refetch,
    isInitialLoading,
    isFirsLoaded: !isLoading,
  });
};

export default useFieldTypeUsages;
