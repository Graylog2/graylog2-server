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

import type { SystemNotifications } from '@graylog/server-api';

import { fetchPeriodically } from 'logic/rest/FetchProvider';
import PaginationURL from 'util/PaginationURL';
import {
  NOTIFICATIONS_QUERY_KEY,
  BADGE_COUNT_KEY,
} from 'components/notifications/constants';

const POLL_INTERVAL = 3000;
const PER_PAGE = 1;

type PaginatedResponse = Awaited<ReturnType<(typeof SystemNotifications)['getPaginated']>>;

const fetchBadgeCount = (): Promise<PaginatedResponse> =>
  fetchPeriodically(
    'GET',
    PaginationURL('/system/notifications/paginated', 1, PER_PAGE, undefined, {
      filters: ['is_read:false'],
    }),
  );

const useNotificationBadgeCount = ({ enabled = true }: { enabled?: boolean } = {}) => {
  const { data, isLoading } = useQuery({
    queryKey: [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY],
    queryFn: fetchBadgeCount,
    refetchInterval: POLL_INTERVAL,
    enabled,
  });

  return {
    data: data?.pagination?.total ?? 0,
    isLoading,
  };
};

export default useNotificationBadgeCount;
