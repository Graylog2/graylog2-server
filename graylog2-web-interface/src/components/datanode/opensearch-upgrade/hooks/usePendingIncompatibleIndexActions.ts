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
import { useEffect, useState } from 'react';

import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import Store from 'logic/local-storage/Store';

import useClusterJobs from './useClusterJobs';
import type { ClusterJobsResult, SystemJobSummary } from './useClusterJobs';

import { ARCHIVE_POLL_INTERVAL_MS } from '../constants';

export const PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY = 'datanode-pending-incompatible-index-actions';

export type PendingIncompatibleIndexAction = {
  action: 'archive-delete';
  indexName: string;
  startedAt: string;
  systemJobId?: string;
  state?: 'archived';
};

export type PendingIndexStatus =
  | { state: 'archiving'; percent: number }
  | { state: 'archived' }
  | { state: 'failed'; message: string };

type ActionResolution =
  | { kind: 'archiving'; percent: number }
  | { kind: 'archived' }
  | { kind: 'failed'; message: string }
  | { kind: 'done' };

type Params = {
  incompatibleIndices: Array<IncompatibleIndex>;
  isLoading: boolean;
  isError: boolean;
  refetch: () => Promise<unknown>;
  canArchive: boolean;
};

const isRunningArchiveSystemJob = (job: SystemJobSummary, archiveSystemJobName: string) =>
  job.name === archiveSystemJobName && String(job.job_status).toLowerCase() === 'running';

const isValidStoredAction = (value: unknown): value is PendingIncompatibleIndexAction => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const candidate = value as Record<string, unknown>;

  return (
    candidate.action === 'archive-delete' &&
    typeof candidate.indexName === 'string' &&
    typeof candidate.startedAt === 'string' &&
    !Number.isNaN(Date.parse(candidate.startedAt)) &&
    (candidate.systemJobId === undefined || typeof candidate.systemJobId === 'string') &&
    (candidate.state === undefined || candidate.state === 'archived')
  );
};

const readStoredActions = (): Array<PendingIncompatibleIndexAction> => {
  const stored = Store.get(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY);

  return Array.isArray(stored) ? stored.filter(isValidStoredAction) : [];
};

const storeActions = (actions: Array<PendingIncompatibleIndexAction>) => {
  try {
    Store.set(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY, actions);
  } catch {
    // Ignore write failures (e.g. storage full / disabled) — tracking degrades to this session only.
  }
};

const resolveAction = (
  action: PendingIncompatibleIndexAction,
  { jobsById, jobsUpdatedAt }: ClusterJobsResult,
): ActionResolution => {
  if (action.state === 'archived') {
    return { kind: 'archived' };
  }

  const job = action.systemJobId ? jobsById.get(action.systemJobId) : undefined;

  if (job?.job_status === 'error') {
    // `||` (not `??`) on purpose: an empty `info` should fall back to the description.
    return { kind: 'failed', message: job.info || job.description };
  }

  if (job?.job_status === 'complete') {
    return { kind: 'archived' };
  }

  if (job?.job_status === 'cancelled') {
    return { kind: 'done' };
  }

  // A jobs list fetched before the action started cannot prove the job is gone — keep waiting.
  if (action.systemJobId && !job && jobsUpdatedAt > Date.parse(action.startedAt)) {
    return { kind: 'archived' };
  }

  return { kind: 'archiving', percent: job?.percent_complete ?? 0 };
};

const reconcileActions = (
  current: Array<PendingIncompatibleIndexAction>,
  incompatibleIndexNames: Set<string>,
  jobs: Pick<ClusterJobsResult, 'jobsById' | 'jobsUpdatedAt'>,
): Array<PendingIncompatibleIndexAction> => {
  const next = current.flatMap((pendingAction): Array<PendingIncompatibleIndexAction> => {
    if (!incompatibleIndexNames.has(pendingAction.indexName)) {
      return [];
    }

    const resolution = resolveAction(pendingAction, jobs);

    if (resolution.kind === 'done') {
      return [];
    }

    if (resolution.kind === 'archived' && pendingAction.state !== 'archived') {
      return [{ ...pendingAction, state: 'archived' }];
    }

    return [pendingAction];
  });

  const unchanged =
    next.length === current.length && next.every((pendingAction, index) => pendingAction === current[index]);

  return unchanged ? current : next;
};

const usePendingIncompatibleIndexActions = ({ incompatibleIndices, isLoading, isError, refetch, canArchive }: Params) => {
  const archive = useIndexArchive();
  const [pendingActions, setPendingActions] = useState<Array<PendingIncompatibleIndexAction>>(readStoredActions);

  const incompatibleIndexNames = new Set(incompatibleIndices.map((index) => index.index_name));
  const trackedActions = pendingActions.filter((pendingAction) => incompatibleIndexNames.has(pendingAction.indexName));
  const activeTrackedActions = trackedActions.filter((pendingAction) => pendingAction.state !== 'archived');
  const hasActiveTrackedActions = activeTrackedActions.length > 0;
  const {
    jobsById,
    jobsUpdatedAt,
    refetch: refetchClusterJobs,
  } = useClusterJobs({ enabled: canArchive || hasActiveTrackedActions, poll: hasActiveTrackedActions });

  const isArchiveJobRunning =
    !!archive && Array.from(jobsById.values()).some((job) => isRunningArchiveSystemJob(job, archive.archiveSystemJobName));

  const pendingIndexStatuses = new Map<string, PendingIndexStatus>();
  trackedActions.forEach((pendingAction) => {
    const resolution = resolveAction(pendingAction, { jobsById, jobsUpdatedAt });

    if (resolution.kind === 'archiving') {
      pendingIndexStatuses.set(pendingAction.indexName, { state: 'archiving', percent: resolution.percent });
    } else if (resolution.kind === 'archived') {
      pendingIndexStatuses.set(pendingAction.indexName, { state: 'archived' });
    } else if (resolution.kind === 'failed') {
      pendingIndexStatuses.set(pendingAction.indexName, { state: 'failed', message: resolution.message });
    }
  });

  const addArchiveDeleteAction = ({ indexName, systemJobId }: { indexName: string; systemJobId?: string }) => {
    setPendingActions((current) => [
      ...current.filter((pendingAction) => pendingAction.indexName !== indexName),
      { action: 'archive-delete', indexName, systemJobId, startedAt: new Date().toISOString() },
    ]);
  };

  // Guarded state adjustment during render instead of an effect:
  // https://react.dev/learn/you-might-not-need-an-effect#adjusting-some-state-when-a-prop-changes
  if (!isLoading && !isError) {
    const reconciled = reconcileActions(pendingActions, incompatibleIndexNames, { jobsById, jobsUpdatedAt });

    if (reconciled !== pendingActions) {
      setPendingActions(reconciled);
    }
  }

  useEffect(() => {
    storeActions(pendingActions);
  }, [pendingActions]);

  // Plain interval: react-query's refetchInterval would need a state round-trip for a flag derived here.
  useEffect(() => {
    if (!hasActiveTrackedActions) {
      return undefined;
    }

    const polling = window.setInterval(() => {
      refetch();
    }, ARCHIVE_POLL_INTERVAL_MS);

    return () => window.clearInterval(polling);
  }, [hasActiveTrackedActions, refetch]);

  return { pendingIndexStatuses, addArchiveDeleteAction, isArchiveJobRunning, refetchClusterJobs };
};

export default usePendingIncompatibleIndexActions;
