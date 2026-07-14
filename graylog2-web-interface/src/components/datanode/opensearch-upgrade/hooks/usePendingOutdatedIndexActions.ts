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
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import Store from 'logic/local-storage/Store';

import useClusterJobs from './useClusterJobs';
import type { ClusterJobsResult, SystemJobSummary } from './useClusterJobs';

import { ARCHIVE_POLL_INTERVAL_MS } from '../constants';

export const PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY = 'datanode-pending-outdated-index-actions';

export type PendingOutdatedIndexAction = {
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
  outdatedIndices: Array<OutdatedIndex>;
  isLoading: boolean;
  isError: boolean;
  refetch: () => Promise<unknown>;
  canArchive: boolean;
};

const ARCHIVE_CREATE_SYSTEM_JOB = 'org.graylog.plugins.archive.job.ArchiveCreateSystemJob';

const isRunningArchiveSystemJob = (job: SystemJobSummary) =>
  job.name === ARCHIVE_CREATE_SYSTEM_JOB && String(job.job_status).toLowerCase() === 'running';

const isValidStoredAction = (value: unknown): value is PendingOutdatedIndexAction => {
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

const readStoredActions = (): Array<PendingOutdatedIndexAction> => {
  const stored = Store.get(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY);

  return Array.isArray(stored) ? stored.filter(isValidStoredAction) : [];
};

const storeActions = (actions: Array<PendingOutdatedIndexAction>) => {
  try {
    Store.set(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY, actions);
  } catch {
    // Ignore write failures (e.g. storage full / disabled) — tracking degrades to this session only.
  }
};

const resolveAction = (
  action: PendingOutdatedIndexAction,
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
  current: Array<PendingOutdatedIndexAction>,
  outdatedIndexNames: Set<string>,
  jobs: Pick<ClusterJobsResult, 'jobsById' | 'jobsUpdatedAt'>,
): Array<PendingOutdatedIndexAction> => {
  const next = current.flatMap((pendingAction): Array<PendingOutdatedIndexAction> => {
    if (!outdatedIndexNames.has(pendingAction.indexName)) {
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

const usePendingOutdatedIndexActions = ({ outdatedIndices, isLoading, isError, refetch, canArchive }: Params) => {
  const [pendingActions, setPendingActions] = useState<Array<PendingOutdatedIndexAction>>(readStoredActions);

  const outdatedIndexNames = useMemo(() => new Set(outdatedIndices.map((index) => index.index_name)), [outdatedIndices]);
  const trackedActions = useMemo(
    () => pendingActions.filter((pendingAction) => outdatedIndexNames.has(pendingAction.indexName)),
    [outdatedIndexNames, pendingActions],
  );
  const activeTrackedActions = useMemo(
    () => trackedActions.filter((pendingAction) => pendingAction.state !== 'archived'),
    [trackedActions],
  );
  const hasActiveTrackedActions = activeTrackedActions.length > 0;
  const {
    jobsById,
    jobsUpdatedAt,
    refetch: refetchClusterJobs,
  } = useClusterJobs({ enabled: canArchive || hasActiveTrackedActions, poll: hasActiveTrackedActions });

  const isArchiveJobRunning = useMemo(
    () => Array.from(jobsById.values()).some(isRunningArchiveSystemJob),
    [jobsById],
  );

  const pendingIndexStatuses = useMemo(() => {
    const statuses = new Map<string, PendingIndexStatus>();

    trackedActions.forEach((pendingAction) => {
      const resolution = resolveAction(pendingAction, { jobsById, jobsUpdatedAt });

      if (resolution.kind === 'archiving') {
        statuses.set(pendingAction.indexName, { state: 'archiving', percent: resolution.percent });
      } else if (resolution.kind === 'archived') {
        statuses.set(pendingAction.indexName, { state: 'archived' });
      } else if (resolution.kind === 'failed') {
        statuses.set(pendingAction.indexName, { state: 'failed', message: resolution.message });
      }
    });

    return statuses;
  }, [jobsById, jobsUpdatedAt, trackedActions]);

  const addArchiveDeleteAction = useCallback(
    ({ indexName, systemJobId }: { indexName: string; systemJobId?: string }) => {
      setPendingActions((current) => [
        ...current.filter((pendingAction) => pendingAction.indexName !== indexName),
        { action: 'archive-delete', indexName, systemJobId, startedAt: new Date().toISOString() },
      ]);
    },
    [],
  );

  // Guarded state adjustment during render instead of an effect:
  // https://react.dev/learn/you-might-not-need-an-effect#adjusting-some-state-when-a-prop-changes
  if (!isLoading && !isError) {
    const reconciled = reconcileActions(pendingActions, outdatedIndexNames, { jobsById, jobsUpdatedAt });

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
      void refetch();
    }, ARCHIVE_POLL_INTERVAL_MS);

    return () => window.clearInterval(polling);
  }, [hasActiveTrackedActions, refetch]);

  return { pendingIndexStatuses, addArchiveDeleteAction, isArchiveJobRunning, refetchClusterJobs };
};

export default usePendingOutdatedIndexActions;
