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
import type { Attribute } from 'stores/PaginationTypes';
import type { FieldTypeUsage } from 'views/logic/fieldactions/ChangeFieldType/types';
import { SystemIndexSetsTypes } from '@graylog/server-api';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type Options = {
  enabled: boolean,
}

type SearchParams = {
    page: number,
    pageSize: number,
    sort: {
        attributeId?: 'index_set_id' | 'index_set_title',
        direction: 'asc' | 'desc'
    }
}

const fetchFieldTypeUsages = async ({ field, streams }: { field: string, streams: Array<string>}, searchParams: SearchParams) => {
  const { sort: { attributeId, direction }, page, pageSize } = searchParams;
  const body = { field, streams: streams.length ? streams : undefined };

  return SystemIndexSetsTypes.fieldTypeSummaries(
    body, attributeId, page, pageSize, direction).then(
    ({ elements, total, attributes }) => ({
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
      pagination: { total },
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
