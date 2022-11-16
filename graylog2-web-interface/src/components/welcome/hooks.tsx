import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import { activities, DEFAULT_PAGINATION, lastOpen } from 'components/welcome/helpers';

export const urlPrefix = '/';
export const LAST_OPEN_QUERY_KEY = 'last_open_query_key';
export const PINNED_ITEMS_QUERY_KEY = 'pinned_items_query_key';
export const RECENT_ACTIONS_QUERY_KEY = 'recent_actions_query_key';

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const fetchLastOpen = async ({ page, per_page }) => {
  const url = PaginationURL(`${urlPrefix}/ttttt/paginated`, page, per_page, '');

  // return fetch('GET', qualifyUrl(url));
  await delay(700);

  return Promise.resolve({ lastOpen: lastOpen, pagination: { total: 10 } });
};

const fetchPinnedItems = async ({ page, per_page }) => {
  const url = PaginationURL(`${urlPrefix}/ttttt/paginated`, page, per_page, '');

  // return fetch('GET', qualifyUrl(url));
  await delay(500);

  return Promise.resolve({ pinnedItems: lastOpen, pagination: { total: 10 } });
};

const fetchRecentActivities = async ({ page, per_page }) => {
  const url = PaginationURL(`${urlPrefix}/ttttt/paginated`, page, per_page, '');
  // return fetch('GET', qualifyUrl(url));
  await delay(1000);

  return Promise.resolve({ resentActivities: activities, pagination: { total: 10 } });
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
