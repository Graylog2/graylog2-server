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

// Classify a pending action from its backend system job:
// - error                          → failed   (kept visible so the user can retry)
// - complete                        → archived (job finished, but the index is still outdated)
// - cancelled                       → done     (job stopped — stop tracking and allow retry)
// - has a job id but it's absent from a jobs list fetched AFTER the action started → archived (finished & cleared)
// - running, jobs list predates the action, or we have no job id to poll → archiving (keep waiting; a
//   no-job-id action is only cleared once its index leaves the outdated list)
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

  // Only conclude the job is gone if we actually have a job id and have polled the jobs list since this
  // action started; otherwise the list may predate it (stale cache / concurrent add) or we never had a job
  // to track — in both cases keep waiting.
  if (action.systemJobId && !job && jobsUpdatedAt > Date.parse(action.startedAt)) {
    return { kind: 'archived' };
  }

  return { kind: 'archiving', percent: job?.percent_complete ?? 0 };
};

// Reconcile tracked actions with reality: drop actions whose index is gone (deleted), and materialize the
// archived state once a job has finished but its index is still outdated. Returns `current` unchanged
// (referentially) when there is nothing to do, so callers can use identity to gate state updates.
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

/**
 * Tracks long-running "archive and delete" actions on outdated indices. Each action's real progress comes
 * from its backend system job (polled via {@link useClusterJobs}): a running job reports progress, an errored
 * job surfaces as failed, and a finished job keeps an "archived" row state if the index is still outdated
 * (e.g. the backend could not delete the current write index). Persisted in localStorage so progress survives
 * reloads.
 */
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
  // One snapshot for archive-capable users catches jobs started elsewhere (other sessions, retention);
  // continuous polling is reserved for actions this session is actually tracking.
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

  // Adjust state during render (guarded by referential equality) instead of in an effect: this both avoids
  // an extra committed render per change and keeps setState out of effects, following
  // https://react.dev/learn/you-might-not-need-an-effect#adjusting-some-state-when-a-prop-changes
  if (!isLoading && !isError) {
    const reconciled = reconcileActions(pendingActions, outdatedIndexNames, { jobsById, jobsUpdatedAt });

    if (reconciled !== pendingActions) {
      setPendingActions(reconciled);
    }
  }

  // Persisting is a pure external-system sync, which is what effects are for. It also covers additions from
  // addArchiveDeleteAction, keeping that a plain state update.
  useEffect(() => {
    storeActions(pendingActions);
  }, [pendingActions]);

  // Refresh the outdated indices while actions are in flight so completed ones drop off. A plain interval
  // instead of react-query's refetchInterval on purpose: the poll flag derives from this hook's own output,
  // which itself consumes the query's data — feeding it back into useOutdatedIndices would need a state
  // round-trip through the parent. The interval stops as soon as nothing is actively tracked.
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
