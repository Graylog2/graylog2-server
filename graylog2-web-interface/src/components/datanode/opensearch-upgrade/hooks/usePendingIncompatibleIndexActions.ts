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

const REINDEX_JOB_TYPE = 'reindex-outdated-index-v1';

export type PendingIncompatibleIndexAction = {
  action: 'archive-delete' | 'reindex';
  indexName: string;
  startedAt: string;
  systemJobId?: string;
};

export type PendingIndexStatus =
  | { state: 'archiving'; percent: number }
  | { state: 'reindexing' }
  | { state: 'failed'; message: string; label: string };

type ActionResolution =
  | { kind: 'archiving'; percent: number }
  | { kind: 'reindexing' }
  | { kind: 'failed'; message: string; label: string }
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
    (candidate.action === 'archive-delete' || candidate.action === 'reindex') &&
    typeof candidate.indexName === 'string' &&
    typeof candidate.startedAt === 'string' &&
    !Number.isNaN(Date.parse(candidate.startedAt)) &&
    (candidate.systemJobId === undefined || typeof candidate.systemJobId === 'string') &&
    candidate.state === undefined
  );
};

const readStoredActions = (): Array<PendingIncompatibleIndexAction> => {
  try {
    const stored = Store.get(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY);

    return Array.isArray(stored) ? stored.filter(isValidStoredAction) : [];
  } catch {
    return [];
  }
};

const storeActions = (actions: Array<PendingIncompatibleIndexAction>) => {
  try {
    Store.set(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY, actions);
  } catch {
    // localStorage writes can throw when browser settings or policies block storage access.
    return;
  }
};

const resolveArchiveAction = (
  action: PendingIncompatibleIndexAction,
  { jobsById, jobsUpdatedAt }: ClusterJobsResult,
): ActionResolution => {
  const job = action.systemJobId ? jobsById.get(action.systemJobId) : undefined;

  if (job?.job_status === 'error') {
    return { kind: 'failed', message: job.info || job.description, label: 'Archive failed' };
  }

  if (job?.job_status === 'complete' || job?.job_status === 'cancelled') {
    return { kind: 'done' };
  }

  if (action.systemJobId && !job && jobsUpdatedAt > Date.parse(action.startedAt)) {
    return { kind: 'done' };
  }

  return { kind: 'archiving', percent: job?.percent_complete ?? 0 };
};

const resolveReindexAction = (
  action: PendingIncompatibleIndexAction,
  { jobsById, jobsUpdatedAt }: ClusterJobsResult,
): ActionResolution => {
  const job = Array.from(jobsById.values()).find(
    (candidate) => candidate.name === REINDEX_JOB_TYPE && !!candidate.info?.includes(`<${action.indexName}>`),
  );

  if (job?.job_status === 'error') {
    return { kind: 'failed', message: job.info || job.description, label: 'Reindex failed' };
  }

  if (job?.job_status === 'complete' || job?.job_status === 'cancelled') {
    return { kind: 'done' };
  }

  if (!job && jobsUpdatedAt > Date.parse(action.startedAt)) {
    return { kind: 'done' };
  }

  return { kind: 'reindexing' };
};

const resolveAction = (action: PendingIncompatibleIndexAction, jobs: ClusterJobsResult): ActionResolution =>
  action.action === 'reindex' ? resolveReindexAction(action, jobs) : resolveArchiveAction(action, jobs);

const reconcileActions = (
  current: Array<PendingIncompatibleIndexAction>,
  jobs: Pick<ClusterJobsResult, 'jobsById' | 'jobsUpdatedAt'>,
): Array<PendingIncompatibleIndexAction> => {
  const next = current.flatMap((pendingAction): Array<PendingIncompatibleIndexAction> => {
    const resolution = resolveAction(pendingAction, jobs);

    if (resolution.kind === 'done') {
      return [];
    }

    return [pendingAction];
  });

  const unchanged =
    next.length === current.length && next.every((pendingAction, index) => pendingAction === current[index]);

  return unchanged ? current : next;
};

const usePendingIncompatibleIndexActions = ({
  incompatibleIndices,
  isLoading,
  isError,
  refetch,
  canArchive,
}: Params) => {
  const archive = useIndexArchive();
  const [pendingActions, setPendingActions] = useState<Array<PendingIncompatibleIndexAction>>(readStoredActions);

  const incompatibleIndexNames = new Set(incompatibleIndices.map((index) => index.index_name));
  const trackedActions = pendingActions.filter((pendingAction) => incompatibleIndexNames.has(pendingAction.indexName));
  const hasActiveActions = pendingActions.length > 0;
  const {
    jobsById,
    jobsUpdatedAt,
    refetch: refetchClusterJobs,
  } = useClusterJobs({ enabled: canArchive || hasActiveActions, poll: hasActiveActions });

  const isArchiveJobRunning =
    !!archive &&
    Array.from(jobsById.values()).some((job) => isRunningArchiveSystemJob(job, archive.archiveSystemJobName));

  const pendingIndexStatuses = new Map<string, PendingIndexStatus>();
  trackedActions.forEach((pendingAction) => {
    const resolution = resolveAction(pendingAction, { jobsById, jobsUpdatedAt });

    if (resolution.kind === 'archiving') {
      pendingIndexStatuses.set(pendingAction.indexName, { state: 'archiving', percent: resolution.percent });
    } else if (resolution.kind === 'reindexing') {
      pendingIndexStatuses.set(pendingAction.indexName, { state: 'reindexing' });
    } else if (resolution.kind === 'failed') {
      pendingIndexStatuses.set(pendingAction.indexName, {
        state: 'failed',
        message: resolution.message,
        label: resolution.label,
      });
    }
  });

  const addPendingAction = (action: PendingIncompatibleIndexAction) =>
    setPendingActions((current) => [
      ...current.filter((pendingAction) => pendingAction.indexName !== action.indexName),
      action,
    ]);

  const addArchiveDeleteAction = ({ indexName, systemJobId }: { indexName: string; systemJobId: string }) =>
    addPendingAction({ action: 'archive-delete', indexName, systemJobId, startedAt: new Date().toISOString() });

  const addReindexAction = ({ indexName }: { indexName: string }) =>
    addPendingAction({ action: 'reindex', indexName, startedAt: new Date().toISOString() });

  if (!isLoading && !isError) {
    const reconciled = reconcileActions(pendingActions, { jobsById, jobsUpdatedAt });

    if (reconciled !== pendingActions) {
      setPendingActions(reconciled);
    }
  }

  useEffect(() => {
    storeActions(pendingActions);
  }, [pendingActions]);

  useEffect(() => {
    if (!hasActiveActions) {
      return undefined;
    }

    const polling = window.setInterval(() => {
      refetch();
    }, ARCHIVE_POLL_INTERVAL_MS);

    return () => window.clearInterval(polling);
  }, [hasActiveActions, refetch]);

  return { pendingIndexStatuses, addArchiveDeleteAction, addReindexAction, isArchiveJobRunning, refetchClusterJobs };
};

export default usePendingIncompatibleIndexActions;
