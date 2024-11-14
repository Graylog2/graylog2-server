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

import type { SearchParams } from 'stores/PaginationTypes';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';
import { EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import { defaultOnError } from 'util/conditional/onError';

type Options = {
  enabled: boolean,
}

export const fetchEventNotifications = (searchParams: SearchParams) => EventNotificationsStore.searchPaginated(
  searchParams.page,
  searchParams.pageSize,
  searchParams.query,
  { sort: searchParams?.sort.attributeId, order: searchParams?.sort.direction },
).then(({ elements, pagination, attributes }) => ({
  list: elements,
  pagination,
  attributes,
}));

export const keyFn = (searchParams?: SearchParams | undefined) => (['eventNotifications', 'overview', ...(searchParams ? [searchParams] : [])]);

type EventNotificationsResult = {
  list: Array<EventNotification>,
  pagination: { total: number }
  attributes: Array<{ id: string, title: string, sortable: boolean }>
};

const useEventNotifications = (searchParams: SearchParams, { enabled }: Options = { enabled: true }): {
  data: EventNotificationsResult | undefined,
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery<EventNotificationsResult>(
    keyFn(searchParams),
    () => defaultOnError(fetchEventNotifications(searchParams), 'Loading event notifications failed with status', 'Could not load event notifications'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
  });
};

export default useEventNotifications;
