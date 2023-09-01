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
import type { FieldTypeUsage } from 'views/logic/fieldactions/ChangeFieldType/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type Options = {
  enabled: boolean,
}

const fieldTypeUsagesUrl = qualifyUrl('/dashboards');

const fetchFieldTypeUsages = ({ field, streamIds }: { field: string, streamIds: Array<string>}, searchParams: SearchParams) => {
  const url = PaginationURL(
    fieldTypeUsagesUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  // fetch<PaginatedFieldTypeUsagesResponse>('POST', qualifyUrl(url), {})
  return Promise.resolve({
    elements: Array(100).fill(null).map((_, i) => (
      {
        id: `some id ${i}`,
        indexSet: `Index set name ${i}`,
        streams: ['stream 1', 'stream 2', 'stream 3', 'stream 4', 'stream 5'],
        typeHistory: ['string', 'number', 'date'],
      }
    )),
    total: 100,
    count: 1,
    page: 1,
    per_page: 20,
    attributes: [
      {
        id: 'id',
        searchable: false,
        sortable: false,
        title: 'id',
        type: 'STRING',
        hidden: true,
      },
      {
        id: 'indexSet',
        searchable: true,
        sortable: true,
        title: 'Index set',
        type: 'STRING',
      },
      {
        id: 'streams',
        searchable: true,
        sortable: true,
        title: 'Streams',
        type: 'STRING',
      },
      {
        id: 'typeHistory',
        searchable: true,
        sortable: true,
        title: 'Current type',
        type: 'STRING',
      },
    ],
  }).then(
    ({ elements, total, count, page, per_page: perPage, attributes }) => ({
      list: elements,
      pagination: { total, count, page, perPage },
      attributes,
    }),
  );
};

const useFiledTypeUsages = (searchParams: SearchParams, field: string, { enabled }: Options = { enabled: true }): {
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
    () => fetchFieldTypeUsages(field, searchParams),
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

export default useFiledTypeUsages;
