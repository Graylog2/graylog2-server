import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import type { PaginatedLastOpen, PaginatedPinned, RequestQuery, PaginatedRecentActivities } from 'components/welcome/types';

export const urlPrefix = '/dynamicstartpage';
export const LAST_OPEN_QUERY_KEY = 'last_open_query_key';
export const PINNED_ITEMS_QUERY_KEY = 'pinned_items_query_key';
export const RECENT_ACTIONS_QUERY_KEY = 'recent_actions_query_key';

const fetchLastOpen = async ({ page }: RequestQuery): Promise<PaginatedLastOpen> => {
  const url = PaginationURL(`${urlPrefix}/lastOpened`, page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const fetchPinnedItems = async ({ page }: RequestQuery): Promise<PaginatedPinned> => {
  const url = PaginationURL(`${urlPrefix}/pinnedItems`, page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const fetchRecentActivities = async ({ page }: RequestQuery): Promise<PaginatedRecentActivities> => {
  const url = PaginationURL(`${urlPrefix}/recentActivity`, page, 5, '');

  return fetch('GET', qualifyUrl(url)).then((data) => {
    return data;
  });
};

export const useLastOpen = (pagination) => {
  return useQuery([LAST_OPEN_QUERY_KEY, pagination], () => fetchLastOpen(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading last opened items failed with status: ${errorThrown}`,
        'Could not load last opened items');
    },
    retry: 0,
    initialData: {
      lastOpen: [],
      pagination: DEFAULT_PAGINATION,
    },
  });
};

export const usePinnedItems = (pagination) => {
  return useQuery([PINNED_ITEMS_QUERY_KEY, pagination], () => fetchPinnedItems(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading pinned items failed with status: ${errorThrown}`,
        'Could not load pinned items');
    },
    retry: 0,
    initialData: {
      pinnedItems: [],
      pagination: DEFAULT_PAGINATION,
    },
  });
};

export const useRecentActivities = (pagination) => {
  return useQuery([RECENT_ACTIONS_QUERY_KEY, pagination], () => fetchRecentActivities(pagination), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading recent activities failed with status: ${errorThrown}`,
        'Could not load recent activities');
    },
    retry: 0,
    initialData: {
      resentActivities: [],
      pagination: DEFAULT_PAGINATION,
    },
  });
};
