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

import type { ViewJson } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type { SearchParams, PaginatedListJSON, Attribute } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

export const KEY_PREFIX = ['dashboards', 'overview'];
export const keyFn = (searchParams: SearchParams) => [...KEY_PREFIX, searchParams];

const dashboardsUrl = qualifyUrl('/dashboards');

type PaginatedDashboardsResponse = PaginatedListJSON & {
  elements: Array<ViewJson>,
  attributes: Array<Attribute>,
};

type Options = {
  enabled?: boolean,
}

type SearchParamsForDashboards = SearchParams & {
  scope: 'read' | 'update',
}

export const fetchDashboards = (searchParams: SearchParamsForDashboards) => {
  const url = PaginationURL(
    dashboardsUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { sort: searchParams.sort.attributeId, order: searchParams.sort.direction, scope: searchParams.scope });

  return fetch<PaginatedDashboardsResponse>('GET', qualifyUrl(url)).then(
    ({ elements, total, count, page, per_page: perPage, attributes }) => ({
      list: elements.map((item) => View.fromJSON(item)),
      pagination: { total, count, page, perPage },
      attributes,
    }),
  );
};

const useDashboards = (searchParams: SearchParamsForDashboards, { enabled }: Options = { enabled: true }): {
  data: {
    list: Readonly<Array<View>>,
    pagination: { total: number },
    attributes: Array<Attribute>
  },
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    keyFn(searchParams),
    () => defaultOnError(fetchDashboards(searchParams), 'Loading dashboards failed with status', 'Could not load dashboards'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    refetch,
    isInitialLoading,
  });
};

export default useDashboards;
