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
import { useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';

import { SystemNotifications } from '@graylog/server-api';

import type { RequestOptions } from 'routing/request';
import {
  NOTIFICATIONS_QUERY_KEY,
  BADGE_COUNT_KEY,
  TABLE_KEY,
} from 'components/notifications/constants';

const POLL_INTERVAL = 3000;
const NO_SESSION_EXT: RequestOptions = { requestShouldExtendSession: false };

const useNotificationBadgeCount = ({ enabled = true }: { enabled?: boolean } = {}) => {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY],
    queryFn: () => SystemNotifications.getUnreadCount(NO_SESSION_EXT),
    refetchInterval: POLL_INTERVAL,
    enabled,
  });

  const count = data ?? 0;
  const previous = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (data === undefined) return;
    if (previous.current !== undefined && previous.current !== count) {
      queryClient.invalidateQueries({ queryKey: [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] });
    }
    previous.current = count;
  }, [count, data, queryClient]);

  return { data: count, isLoading };
};

export default useNotificationBadgeCount;
