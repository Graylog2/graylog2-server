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

import { DashboardsActions } from 'views/stores/DashboardsStore';
import UserNotification from 'util/UserNotification';
import type { ViewJson } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type { SearchParams, PaginatedListJSON } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';

type PaginatedDashboardsResponse = PaginatedListJSON & {
  views: Array<ViewJson>,
};

const dashboardsUrl = qualifyUrl('/dashboards');

const fetchDashboards = (searchParams: SearchParams) => {
  const url = PaginationURL(
    dashboardsUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  return fetch<PaginatedDashboardsResponse>('GET', qualifyUrl(url)).then(
    ({ views, total, count, page, per_page: perPage }) => ({
      list: views.map((item) => View.fromJSON(item)),
      pagination: { total, count, page, perPage },
    }),
  );
};

const useDashboards = (searchParams: SearchParams): {
  data: {
    list: Readonly<Array<View>>,
    pagination: { total: number }
  } | undefined,
  refetch: () => void
} => {
  const { data, refetch } = useQuery(
    ['dashboards', 'overview', searchParams],
    () => fetchDashboards(searchParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading dashboards failed with status: ${errorThrown}`,
          'Could not load dashboards');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
  });
};

export default useDashboards;
