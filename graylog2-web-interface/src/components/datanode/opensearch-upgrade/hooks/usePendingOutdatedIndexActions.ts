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

import useClusterJobs from './useClusterJobs';
import type { ClusterJobsResult } from './useClusterJobs';

import { ARCHIVE_POLL_INTERVAL_MS } from '../constants';

export const PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY = 'datanode-pending-outdated-index-actions';

export type PendingOutdatedIndexAction = {
  action: 'archive-delete';
  indexName: string;
  startedAt: string;
  systemJobId?: string;
};

export type PendingIndexStatus =
  | { state: 'archiving'; percent: number }
  | { state: 'failed'; message: string };

type ActionResolution = { kind: 'archiving'; percent: number } | { kind: 'failed'; message: string } | { kind: 'done' };

type Params = {
  outdatedIndices: Array<OutdatedIndex>;
  isLoading: boolean;
  isError: boolean;
  refetch: () => Promise<unknown>;
};

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
    (candidate.systemJobId === undefined || typeof candidate.systemJobId === 'string')
  );
};

const readStoredActions = (): Array<PendingOutdatedIndexAction> => {
  try {
    const stored = window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY);
    const parsed = stored ? JSON.parse(stored) : [];

    return Array.isArray(parsed) ? parsed.filter(isValidStoredAction) : [];
  } catch {
    return [];
  }
};

const storeActions = (actions: Array<PendingOutdatedIndexAction>) => {
  try {
    window.localStorage.setItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY, JSON.stringify(actions));
  } catch {
    // Ignore write failures (e.g. storage full / disabled) — tracking degrades to this session only.
  }
};

// Classify a pending action from its backend system job:
// - error                          → failed (kept visible so the user can retry)
// - complete / cancelled            → done   (job finished — stop tracking)
// - has a job id but it's absent from a jobs list fetched AFTER the action started → done (finished & cleared)
// - running, jobs list predates the action, or we have no job id to poll → archiving (keep waiting; a
//   no-job-id action is only cleared once its index leaves the outdated list)
const resolveAction = (
  action: PendingOutdatedIndexAction,
  { jobsById, jobsUpdatedAt }: ClusterJobsResult,
): ActionResolution => {
  const job = action.systemJobId ? jobsById.get(action.systemJobId) : undefined;

  if (job?.job_status === 'error') {
    // `||` (not `??`) on purpose: an empty `info` should fall back to the description.
    return { kind: 'failed', message: job.info || job.description };
  }

  if (job?.job_status === 'complete' || job?.job_status === 'cancelled') {
    return { kind: 'done' };
  }

  // Only conclude the job is gone if we actually have a job id and have polled the jobs list since this
  // action started; otherwise the list may predate it (stale cache / concurrent add) or we never had a job
  // to track — in both cases keep waiting.
  if (action.systemJobId && !job && jobsUpdatedAt > Date.parse(action.startedAt)) {
    return { kind: 'done' };
  }

  return { kind: 'archiving', percent: job?.percent_complete ?? 0 };
};

/**
 * Tracks long-running "archive and delete" actions on outdated indices. Each action's real progress comes
 * from its backend system job (polled via {@link useClusterJobs}): a running job reports progress, an errored
 * job surfaces as failed, and a finished job stops tracking — so a row never lingers on a fake 0% once its
 * job is gone (e.g. archiving the active write index, which cannot be deleted). Persisted in localStorage so
 * progress survives reloads.
 */
const usePendingOutdatedIndexActions = ({ outdatedIndices, isLoading, isError, refetch }: Params) => {
  const [pendingActions, setPendingActions] = useState<Array<PendingOutdatedIndexAction>>(readStoredActions);

  const outdatedIndexNames = useMemo(() => new Set(outdatedIndices.map((index) => index.index_name)), [outdatedIndices]);
  const trackedActions = useMemo(
    () => pendingActions.filter((pendingAction) => outdatedIndexNames.has(pendingAction.indexName)),
    [outdatedIndexNames, pendingActions],
  );
  const hasTrackedActions = trackedActions.length > 0;
  const { jobsById, jobsUpdatedAt } = useClusterJobs(hasTrackedActions);

  const pendingIndexStatuses = useMemo(() => {
    const statuses = new Map<string, PendingIndexStatus>();

    trackedActions.forEach((pendingAction) => {
      const resolution = resolveAction(pendingAction, { jobsById, jobsUpdatedAt });

      if (resolution.kind === 'archiving') {
        statuses.set(pendingAction.indexName, { state: 'archiving', percent: resolution.percent });
      } else if (resolution.kind === 'failed') {
        statuses.set(pendingAction.indexName, { state: 'failed', message: resolution.message });
      }
    });

    return statuses;
  }, [jobsById, jobsUpdatedAt, trackedActions]);

  const addArchiveDeleteAction = useCallback(
    ({ indexName, systemJobId }: { indexName: string; systemJobId?: string }) => {
      setPendingActions((current) => {
        const next: Array<PendingOutdatedIndexAction> = [
          ...current.filter((pendingAction) => pendingAction.indexName !== indexName),
          { action: 'archive-delete', indexName, systemJobId, startedAt: new Date().toISOString() },
        ];
        storeActions(next);

        return next;
      });
    },
    [],
  );

  // Stop tracking an action once its index is gone (deleted) or its job has finished.
  useEffect(() => {
    if (isLoading || isError) {
      return;
    }

    setPendingActions((current) => {
      const next = current.filter(
        (pendingAction) =>
          outdatedIndexNames.has(pendingAction.indexName) &&
          resolveAction(pendingAction, { jobsById, jobsUpdatedAt }).kind !== 'done',
      );

      if (next.length === current.length) {
        return current;
      }

      storeActions(next);

      return next;
    });
  }, [isError, isLoading, jobsById, jobsUpdatedAt, outdatedIndexNames]);

  // Refresh the outdated indices while actions are in flight so completed ones drop off.
  useEffect(() => {
    if (!hasTrackedActions) {
      return undefined;
    }

    const polling = window.setInterval(() => {
      void refetch();
    }, ARCHIVE_POLL_INTERVAL_MS);

    return () => window.clearInterval(polling);
  }, [hasTrackedActions, refetch]);

  return { pendingIndexStatuses, addArchiveDeleteAction };
};

export default usePendingOutdatedIndexActions;
