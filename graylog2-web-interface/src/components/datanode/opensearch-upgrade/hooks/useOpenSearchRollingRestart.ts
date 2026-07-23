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

import { DataNodeRollingRestart } from '@graylog/server-api';

import FetchError from 'logic/errors/FetchError';
import { defaultOnError } from 'util/conditional/onError';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import type { RollingRestartJob } from '../rollingRestartTypes';
import { isRollingRestartTerminalState } from '../rollingRestartTypes';

const ROLLING_RESTART_QUERY_KEY = ['opensearch-upgrade', 'rolling-restart'];
const ROLLING_RESTART_STATUS_REFETCH_INTERVAL = 5000;

type RollingRestartErrorBody = {
  error?: string;
  failed_checks?: Array<string>;
  message?: string;
};

const fetchCurrentRollingRestart = () =>
  DataNodeRollingRestart.current({ requestShouldExtendSession: false }) as Promise<RollingRestartJob | null>;

const startRollingRestartRequest = (force: boolean) =>
  DataNodeRollingRestart.start({ force }) as Promise<RollingRestartJob>;

const rollingRestartErrorBody = (error: unknown): RollingRestartErrorBody | undefined =>
  error instanceof FetchError ? error.additional?.body : undefined;

const isForceableFailedCheck = (failedCheck: string) => failedCheck.includes('force=true');

const displayFailedCheck = (failedCheck: string) => failedCheck.replace(/\s*\(pass force=true to override\)/, '');

export const rollingRestartStartError = (error: unknown) => {
  const body = rollingRestartErrorBody(error);
  const failedChecks = body?.failed_checks ?? [];
  const displayFailedChecks = failedChecks.map(displayFailedCheck);

  if (failedChecks.length) {
    return {
      canRetryWithForce: failedChecks.every(isForceableFailedCheck),
      failedChecks: displayFailedChecks,
      message: displayFailedChecks.join('\n'),
    };
  }

  return {
    canRetryWithForce: false,
    failedChecks,
    message: body?.error ?? body?.message ?? extractErrorMessage(error),
  };
};

export const useCurrentRollingRestart = () => {
  const { data, isInitialLoading, refetch } = useQuery<RollingRestartJob | null>({
    queryKey: ROLLING_RESTART_QUERY_KEY,
    queryFn: () =>
      defaultOnError(
        fetchCurrentRollingRestart(),
        'Loading OpenSearch rolling upgrade status failed',
        'Could not load OpenSearch rolling upgrade status',
      ),
    refetchInterval: (query) => {
      const rollingRestart = query.state.data;

      if (isRollingRestartTerminalState(rollingRestart?.data?.sm_state)) {
        return false;
      }

      return ROLLING_RESTART_STATUS_REFETCH_INTERVAL;
    },
  });

  return { data, isLoading: isInitialLoading, refetch };
};

const useOpenSearchRollingRestart = () => {
  const queryClient = useQueryClient();
  const { data, isLoading, refetch } = useCurrentRollingRestart();

  const { mutateAsync: startRollingRestart, isPending: isStartingRollingRestart } = useMutation({
    mutationFn: (force: boolean = false) => startRollingRestartRequest(force),
    onSuccess: (rollingRestart) => queryClient.setQueryData(ROLLING_RESTART_QUERY_KEY, rollingRestart),
    onError: (error, force) => {
      const startError = rollingRestartStartError(error);

      // Overridable failures surface as the caller's force-confirm dialog instead of a toast.
      if (force || !startError.canRetryWithForce) {
        UserNotification.error(startError.message, 'Could not start OpenSearch rolling upgrade');
      }
    },
  });

  const { mutateAsync: resumeRollingRestart, isPending: isResumingRollingRestart } = useMutation({
    // The resume response is a bare job trigger without the rolling-restart payload, so refetch instead.
    mutationFn: () => DataNodeRollingRestart.resume(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ROLLING_RESTART_QUERY_KEY });
      UserNotification.success('OpenSearch rolling upgrade was resumed.');
    },
    onError: (error) => {
      UserNotification.error(extractErrorMessage(error), 'Could not resume OpenSearch rolling upgrade');
    },
  });

  return {
    data,
    isLoading,
    isResumingRollingRestart,
    isStartingRollingRestart,
    refetch,
    resumeRollingRestart,
    startRollingRestart,
  };
};

export default useOpenSearchRollingRestart;
