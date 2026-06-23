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

import FetchError from 'logic/errors/FetchError';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import { rollingRestartMockOverride } from '../mockRollingRestart';
import type { RollingRestartJob } from '../rollingRestartTypes';
import { isRollingRestartTerminalState } from '../rollingRestartTypes';

const ROLLING_RESTART_QUERY_KEY = ['opensearch-upgrade', 'rolling-restart'];
const ROLLING_RESTART_STATUS_REFETCH_INTERVAL = 5000;
const ROLLING_RESTART_URL = '/datanodes/restart';

type RollingRestartErrorBody = {
  error?: string;
  failed_checks?: Array<string>;
  message?: string;
};

const fetchCurrentRollingRestart = () => fetch<RollingRestartJob | null>('GET', qualifyUrl(ROLLING_RESTART_URL));

const startRollingRestartRequest = (force: boolean) =>
  fetch<RollingRestartJob>('POST', qualifyUrl(ROLLING_RESTART_URL), { force });

const abortRollingRestartRequest = () => fetch<RollingRestartJob>('POST', qualifyUrl(`${ROLLING_RESTART_URL}/abort`));

const resumeRollingRestartRequest = () => fetch<RollingRestartJob>('POST', qualifyUrl(`${ROLLING_RESTART_URL}/resume`));

const fetchMockRollingRestart = () => Promise.resolve(rollingRestartMockOverride ?? null);

const startMockRollingRestart = () => Promise.resolve(rollingRestartMockOverride as RollingRestartJob);

const updateMockRollingRestart = (updater: (job: RollingRestartJob) => RollingRestartJob) =>
  Promise.resolve(updater(rollingRestartMockOverride as RollingRestartJob));

const abortMockRollingRestart = () =>
  updateMockRollingRestart((job) => ({
    ...job,
    data: job.data ? { ...job.data, abort_requested: true } : job.data,
  }));

const resumeMockRollingRestart = () =>
  updateMockRollingRestart((job) => ({
    ...job,
    data: job.data
      ? {
          ...job.data,
          sm_state: 'WAITING_GREEN',
          paused_reason: null,
          waiting_green_since: new Date().toISOString(),
        }
      : job.data,
  }));

const rollingRestartErrorBody = (error: unknown): RollingRestartErrorBody | undefined =>
  error instanceof FetchError ? error.additional?.body : undefined;

export const rollingRestartStartError = (error: unknown) => {
  const body = rollingRestartErrorBody(error);
  const failedChecks = body?.failed_checks ?? [];

  if (failedChecks.length) {
    return {
      canRetryWithForce: failedChecks.some((failedCheck) => failedCheck.includes('force=true')),
      failedChecks,
      message: failedChecks.join('\n'),
    };
  }

  return {
    canRetryWithForce: false,
    failedChecks,
    message: body?.error ?? body?.message ?? extractErrorMessage(error),
  };
};

const useOpenSearchRollingRestart = () => {
  const queryClient = useQueryClient();
  const isUsingMockData = !!rollingRestartMockOverride;
  const queryKey = [...ROLLING_RESTART_QUERY_KEY, isUsingMockData];

  const { data, isInitialLoading, refetch } = useQuery({
    queryKey,
    queryFn: () =>
      isUsingMockData
        ? fetchMockRollingRestart()
        : defaultOnError(
            fetchCurrentRollingRestart(),
            'Loading OpenSearch rolling restart status failed',
            'Could not load OpenSearch rolling restart status',
          ),
    refetchInterval: (query) => {
      const rollingRestart = query.state.data;

      if (isUsingMockData || isRollingRestartTerminalState(rollingRestart?.data?.sm_state)) {
        return false;
      }

      return ROLLING_RESTART_STATUS_REFETCH_INTERVAL;
    },
  });

  const { mutateAsync: startRollingRestart, isPending: isStartingRollingRestart } = useMutation({
    mutationFn: (force: boolean = false) =>
      isUsingMockData ? startMockRollingRestart() : startRollingRestartRequest(force),
    onSuccess: (rollingRestart) => queryClient.setQueryData(queryKey, rollingRestart),
    onError: (error, force) => {
      const startError = rollingRestartStartError(error);

      // When the failure is overridable with force, the caller shows a confirmation dialog instead —
      // skip the toast to avoid signalling the same thing twice. A failed forced retry still toasts.
      if (force || !startError.canRetryWithForce) {
        UserNotification.error(startError.message, 'Could not start OpenSearch rolling upgrade');
      }
    },
  });

  const { mutateAsync: abortRollingRestart, isPending: isAbortingRollingRestart } = useMutation({
    mutationFn: () => (isUsingMockData ? abortMockRollingRestart() : abortRollingRestartRequest()),
    onSuccess: (rollingRestart) => {
      queryClient.setQueryData(queryKey, rollingRestart);
      UserNotification.success('OpenSearch rolling upgrade abort was requested.');
    },
    onError: (error) => {
      UserNotification.error(extractErrorMessage(error), 'Could not abort OpenSearch rolling upgrade');
    },
  });

  const { mutateAsync: resumeRollingRestart, isPending: isResumingRollingRestart } = useMutation({
    mutationFn: () => (isUsingMockData ? resumeMockRollingRestart() : resumeRollingRestartRequest()),
    onSuccess: (rollingRestart) => {
      queryClient.setQueryData(queryKey, rollingRestart);
      UserNotification.success('OpenSearch rolling upgrade was resumed.');
    },
    onError: (error) => {
      UserNotification.error(extractErrorMessage(error), 'Could not resume OpenSearch rolling upgrade');
    },
  });

  return {
    abortRollingRestart,
    data,
    isAbortingRollingRestart,
    isLoading: isInitialLoading,
    isResumingRollingRestart,
    isStartingRollingRestart,
    refetch,
    resumeRollingRestart,
    startRollingRestart,
  };
};

export default useOpenSearchRollingRestart;
