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

import { Collectors } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import type { PendingChangesResponse } from '../types';

export const pendingChangesKey = (instanceUid: string) => [
  'collectors',
  'instances',
  instanceUid,
  'pending_changes',
];

const fetchPendingChanges = (instanceUid: string): Promise<PendingChangesResponse> =>
  Collectors.instancePendingChanges(instanceUid) as Promise<PendingChangesResponse>;

const useInstancePendingChanges = (
  instanceUid: string,
): { data: PendingChangesResponse | undefined; isLoading: boolean } =>
  useQuery<PendingChangesResponse>({
    queryKey: pendingChangesKey(instanceUid),
    queryFn: () =>
      defaultOnError(
        fetchPendingChanges(instanceUid),
        'Loading pending changes failed with status',
        'Could not load pending changes',
      ),
    // Same cadence as the instances table, so an open drawer clears on its own
    // once the collector has applied its changes.
    refetchInterval: 30000,
  });

export default useInstancePendingChanges;
