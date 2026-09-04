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

import { SystemNotifications } from '@graylog/server-api';

import type { RequestOptions } from 'routing/request';
import { NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY } from 'components/notifications/constants';

const POLL_INTERVAL = 3000;
const NO_SESSION_EXT: RequestOptions = { requestShouldExtendSession: false };

const useNotificationBadgeCount = ({ enabled = true }: { enabled?: boolean } = {}) => {
  const { data, isLoading } = useQuery({
    queryKey: [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY],
    queryFn: () => SystemNotifications.getCount(NO_SESSION_EXT),
    refetchInterval: POLL_INTERVAL,
    enabled,
  });

  return { data: data ?? 0, isLoading };
};

export default useNotificationBadgeCount;
