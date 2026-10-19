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

import i18n from 'i18n';
import { defaultOnError } from 'util/conditional/onError';

import useCollectorRefetchInterval from './useCollectorRefetchInterval';

import type { PendingChangesResponse } from '../types';

export const pendingChangesKey = (instanceUid: string) => ['collectors', 'instances', instanceUid, 'pending_changes'];

const fetchPendingChanges = (instanceUid: string): Promise<PendingChangesResponse> =>
  Collectors.instancePendingChanges(instanceUid, {
    requestShouldExtendSession: false,
  }) as Promise<PendingChangesResponse>;

const useInstancePendingChanges = (
  instanceUid: string,
): { data: PendingChangesResponse | undefined; isLoading: boolean; isError: boolean } => {
  // Same cadence as the instances table, so an open drawer clears on its own
  // once the collector has applied its changes.
  const refetchInterval = useCollectorRefetchInterval();

  const { data, isLoading, isError } = useQuery<PendingChangesResponse>({
    queryKey: pendingChangesKey(instanceUid),
    queryFn: () =>
      defaultOnError(
        fetchPendingChanges(instanceUid),
        i18n.t('load_errors.pending_changes.message', { ns: 'collectors' }),
        i18n.t('load_errors.pending_changes.title', { ns: 'collectors' }),
      ),
    refetchInterval,
  });

  return { data, isLoading, isError };
};

export default useInstancePendingChanges;
