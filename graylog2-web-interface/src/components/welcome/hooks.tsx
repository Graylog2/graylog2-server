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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import type {
  PaginatedLastOpened,
  PaginatedPinnedItems,
  RequestQuery,
  PaginatedRecentActivity,
  PaginatedResponseRecentActivity,
} from 'components/welcome/types';

export const urlPrefix = '/dynamicstartpage';
export const LAST_OPEN_QUERY_KEY = 'last_open_query_key';
export const PINNED_ITEMS_QUERY_KEY = 'pinned_items_query_key';
export const RECENT_ACTIONS_QUERY_KEY = 'recent_actions_query_key';

const fetchLastOpen = async ({ page }: RequestQuery): Promise<PaginatedLastOpened> => {
  const url = PaginationURL(`${urlPrefix}/lastOpened`, page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const fetchPinnedItems = async ({ page }: RequestQuery): Promise<PaginatedPinnedItems> => {
  const url = PaginationURL(`${urlPrefix}/pinnedItems`, page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const fetchRecentActivities = async ({ page }: RequestQuery): Promise<PaginatedRecentActivity> => {
  const url = PaginationURL(`${urlPrefix}/recentActivity`, page, 5, '');

  return fetch('GET', qualifyUrl(url)).then((data: PaginatedResponseRecentActivity): PaginatedRecentActivity => {
    return ({
      page: data.page,
      per_page: data.per_page,
      count: data.count,
      total: data.total,
      recentActivity: data.recentActivity.map((activity) => ({
        id: activity.id,
        title: activity.title,
        timestamp: activity.timestamp,
        activityType: activity.activity_type,
        itemType: activity.item_type,
        itemId: activity.item_id,
      })),
    });
  });
};

export const useLastOpened = (pagination: RequestQuery): { data: PaginatedLastOpened, isFetching: boolean } => {
  return useQuery([LAST_OPEN_QUERY_KEY, pagination], () => fetchLastOpen(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading last opened items failed with status: ${errorThrown}`,
        'Could not load last opened items');
    },
    retry: 0,
    initialData: {
      lastOpened: [],
      ...DEFAULT_PAGINATION,
    },
  });
};

export const usePinnedItems = (pagination: RequestQuery): {data: PaginatedPinnedItems, isFetching: boolean} => {
  return useQuery([PINNED_ITEMS_QUERY_KEY, pagination], () => fetchPinnedItems(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading pinned items failed with status: ${errorThrown}`,
        'Could not load pinned items');
    },
    retry: 0,
    initialData: {
      pinnedItems: [],
      ...DEFAULT_PAGINATION,
    },
  });
};

export const useRecentActivity = (pagination: RequestQuery): { data: PaginatedRecentActivity, isFetching: boolean} => {
  return useQuery([RECENT_ACTIONS_QUERY_KEY, pagination], () => fetchRecentActivities(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading recent activity failed with status: ${errorThrown}`,
        'Could not load recent activity');
    },
    retry: 0,
    initialData: {
      recentActivity: [],
      ...DEFAULT_PAGINATION,
    },
  });
};
