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

import request from 'routing/request';
import { defaultOnError } from 'util/conditional/onError';

import type { RecentActivityResponse } from '../types';

export const ACTIVITY_KEY = ['collectors', 'activity', 'recent'];

// TODO: replace with generated API client after regeneration
const fetchRecentActivity = (): Promise<RecentActivityResponse> =>
  request('GET', '/collectors/activity/recent', null, {}, { Accept: 'application/json' });

export const useRecentActivity = () =>
  useQuery<RecentActivityResponse>({
    queryKey: ACTIVITY_KEY,
    queryFn: () =>
      defaultOnError(
        fetchRecentActivity(),
        'Loading recent activity failed with status',
        'Could not load recent activity',
      ),
    refetchInterval: 30000,
  });
