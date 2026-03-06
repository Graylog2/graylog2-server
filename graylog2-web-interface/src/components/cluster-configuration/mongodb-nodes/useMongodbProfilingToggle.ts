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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';

export const MONGODB_NODES_QUERY_KEY_PREFIX = ['mongodbNodes'] as const;
export const MONGODB_PROFILING_STATUS_QUERY_KEY = [...MONGODB_NODES_QUERY_KEY_PREFIX, 'profilingStatus'] as const;

export type MongodbProfilingStatusByLevel = Partial<Record<'OFF' | 'SLOW_OPS' | 'ALL', number>>;
export type MongodbProfilingState = 'off' | 'enabled' | 'mixed' | 'unknown';
export type MongodbProfilingToggleAction = 'enable' | 'disable';

const fetchMongodbProfilingStatus = () =>
  fetch('GET', qualifyUrl('/system/cluster/mongodb/profiling/status')) as Promise<MongodbProfilingStatusByLevel>;

const enableMongodbProfiling = () => fetch('GET', qualifyUrl('/system/cluster/mongodb/profiling/SLOW_OPS'));
const disableMongodbProfiling = () => fetch('GET', qualifyUrl('/system/cluster/mongodb/profiling/OFF'));

export const getClusterProfilingState = (statusByLevel?: MongodbProfilingStatusByLevel): MongodbProfilingState => {
  if (!statusByLevel) {
    return 'unknown';
  }

  const offCount = statusByLevel.OFF ?? 0;
  const enabledCount = (statusByLevel.SLOW_OPS ?? 0) + (statusByLevel.ALL ?? 0);

  if (offCount <= 0 && enabledCount <= 0) {
    return 'unknown';
  }

  if (offCount > 0 && enabledCount <= 0) {
    return 'off';
  }

  if (enabledCount > 0 && offCount <= 0) {
    return 'enabled';
  }

  return 'mixed';
};

export const getProfilingActionForState = (state: MongodbProfilingState): MongodbProfilingToggleAction =>
  state === 'enabled' ? 'disable' : 'enable';

const actionText = {
  enable: 'enable',
  disable: 'disable',
} as const;

const successMessage = {
  enable: 'MongoDB profiling enabled successfully.',
  disable: 'MongoDB profiling disabled successfully.',
} as const;

const useMongodbProfilingToggle = () => {
  const queryClient = useQueryClient();
  const { data: statusByNode, isLoading: isLoadingStatus } = useQuery({
    queryKey: MONGODB_PROFILING_STATUS_QUERY_KEY,
    queryFn: fetchMongodbProfilingStatus,
  });

  const state = getClusterProfilingState(statusByNode);
  const action = getProfilingActionForState(state);

  const { mutateAsync: toggleProfiling, isPending: isTogglingProfiling } = useMutation({
    mutationFn: (toggleAction: MongodbProfilingToggleAction) =>
      toggleAction === 'enable' ? enableMongodbProfiling() : disableMongodbProfiling(),
    onSuccess: async (_response, toggleAction) => {
      UserNotification.success(successMessage[toggleAction]);
      await queryClient.invalidateQueries({ queryKey: MONGODB_NODES_QUERY_KEY_PREFIX });
    },
    onError: (error, toggleAction) => {
      UserNotification.error(
        `Failed to ${actionText[toggleAction]} MongoDB profiling: ${error}`,
        'Could not update MongoDB profiling.',
      );
    },
  });

  const runToggleAction = async (): Promise<boolean> => {
    try {
      await toggleProfiling(action);

      return true;
    } catch {
      return false;
    }
  };

  return {
    action,
    state,
    profilingStatusByLevel: statusByNode,
    isLoadingStatus,
    isTogglingProfiling,
    runToggleAction,
  };
};

export default useMongodbProfilingToggle;
