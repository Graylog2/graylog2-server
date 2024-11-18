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

import type { PaginatedRecentActivity, PaginatedResponseRecentActivity, RequestQuery } from 'components/welcome/types';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

const urlPrefix = '/startpage';
export const RECENT_ACTIONS_QUERY_KEY = 'recent_actions_query_key';

const fetchRecentActivities = async ({ page }: RequestQuery): Promise<PaginatedRecentActivity> => {
  const url = PaginationURL(`${urlPrefix}/recentActivity`, page, 5, '');

  return fetch('GET', qualifyUrl(url)).then((data: PaginatedResponseRecentActivity): PaginatedRecentActivity => ({
    page: data.page,
    per_page: data.per_page,
    count: data.count,
    total: data.total,
    recentActivity: data.recentActivity.map((activity) => ({
      id: activity.id,
      itemTitle: activity.item_title,
      timestamp: activity.timestamp,
      activityType: activity.activity_type,
      itemGrn: activity.item_grn,
      userName: activity.user_name,
    })),
  }));
};

const useRecentActivity = (pagination: RequestQuery): { data: PaginatedRecentActivity, isFetching: boolean } => useQuery(
  [RECENT_ACTIONS_QUERY_KEY, pagination],
  () => defaultOnError(fetchRecentActivities(pagination), 'Loading recent activity failed with status', 'Could not load recent activity'),
  {
    retry: 0,
    initialData: {
      recentActivity: [],
      ...DEFAULT_PAGINATION,
    },
  });

export default useRecentActivity;
