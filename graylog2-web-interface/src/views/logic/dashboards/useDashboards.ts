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
import type { Sort } from 'components/common/EntityDataTable';
import UserNotification from 'util/UserNotification';
import type View from 'views/logic/views/View';

type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
  sort: Sort
}

const useDashboards = (searchParams: SearchParams): {
  data: {
    list: Readonly<Array<View>>,
    pagination: { total: number }
  } | undefined,
  refetch: () => void
} => {
  const { data, refetch } = useQuery(
    ['streams', 'overview', searchParams],
    () => DashboardsActions.search(searchParams.query, searchParams.page, searchParams.pageSize, searchParams.sort.columnId, searchParams.sort.order),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
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
