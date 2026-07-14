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
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import extractErrorMessage from 'util/extractErrorMessage';

import { BULK_INDEX_ACTION_CONCURRENCY } from './constants';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import { CORE_ACTION_DEFINITIONS, getAvailableActions } from './incompatibleIndexActions';

// Deliberately no 'archive-delete' (bulk would race the single-concurrency backend job) or 'rotate' (row-only).
const BULK_ACTION_ORDER = ['delete', 'reindex-system-index'] as const;

type BulkCapableIndexAction = (typeof BULK_ACTION_ORDER)[number];

type BulkActionCopy = {
  buttonLabel: string;
  confirmTitle: string;
  confirmText: string;
  targetVerb: string;
  successMessage: (count: number) => string;
  partialSuccessTitle: string;
  failureTitle: string;
};

export type BulkIndexActionCandidate = BulkActionCopy & {
  action: BulkCapableIndexAction;
  targetIndices: Array<IncompatibleIndex>;
};

export type BulkIndexActionSuccess = {
  index: IncompatibleIndex;
  response: unknown;
};

export type BulkIndexActionFailure = {
  index: IncompatibleIndex;
  error: unknown;
};

export type BulkIndexActionResult = {
  successes: Array<BulkIndexActionSuccess>;
  failures: Array<BulkIndexActionFailure>;
};

export type BulkIndexActionNotification = {
  type: 'success' | 'warning' | 'error';
  message: string;
  title?: string;
};

const BULK_ACTION_COPY: Record<BulkCapableIndexAction, BulkActionCopy> = {
  delete: {
    buttonLabel: 'Delete all',
    confirmTitle: 'Delete incompatible indices',
    confirmText: 'Delete all',
    targetVerb: 'delete',
    successMessage: (count) => `${count} incompatible ${count === 1 ? 'index was' : 'indices were'} deleted.`,
    partialSuccessTitle: 'Some indices could not be deleted',
    failureTitle: 'Could not delete indices',
  },
  'reindex-system-index': {
    buttonLabel: 'Reindex all',
    confirmTitle: 'Reindex system indices',
    confirmText: 'Reindex all',
    targetVerb: 'reindex',
    successMessage: (count) => `${count} system ${count === 1 ? 'index was' : 'indices were'} reindexed.`,
    partialSuccessTitle: 'Some system indices could not be reindexed',
    failureTitle: 'Could not reindex system indices',
  },
};

// Deleting mid-archive is racy; an already-archived index stays deletable.
const isArchiveInProgress = (pendingStatus: PendingIndexStatus | undefined) => pendingStatus?.state === 'archiving';

export const getBulkIndexActionCandidates = ({
  indices,
  canArchive,
  pendingIndexStatuses,
  archivedIndexNames,
}: {
  indices: Array<IncompatibleIndex>;
  canArchive: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: ReadonlySet<string>;
}): Array<BulkIndexActionCandidate> =>
  BULK_ACTION_ORDER.map((action) => {
    const targetIndices = indices.filter(
      (index) =>
        getAvailableActions(index, canArchive, archivedIndexNames.has(index.index_name)).includes(action) &&
        !isArchiveInProgress(pendingIndexStatuses.get(index.index_name)),
    );

    return {
      action,
      targetIndices,
      ...BULK_ACTION_COPY[action],
    };
  }).filter((candidate) => candidate.targetIndices.length > 0);

// A 403 would trigger FetchProvider's global redirect mid-batch, but the only 403 case (an active
// write index) never reaches a batch — getAvailableActions offers it rotate only.
export const runBulkIndexAction = async ({
  action,
  indices,
}: {
  action: BulkCapableIndexAction;
  indices: Array<IncompatibleIndex>;
}): Promise<BulkIndexActionResult> => {
  const actionDefinition = CORE_ACTION_DEFINITIONS[action];
  const successes: Array<BulkIndexActionSuccess> = [];
  const failures: Array<BulkIndexActionFailure> = [];
  let nextIndex = 0;

  const runNext = (): Promise<void> => {
    const index = indices[nextIndex];
    nextIndex += 1;

    if (!index) {
      return Promise.resolve();
    }

    return Promise.resolve()
      .then(() => actionDefinition.run(index))
      .then((response) => {
        successes.push({ index, response });
      })
      .catch((error) => {
        failures.push({ index, error });
      })
      .then(runNext);
  };

  const workerCount = Math.min(BULK_INDEX_ACTION_CONCURRENCY, indices.length);

  await Promise.all(Array.from({ length: workerCount }, runNext));

  return { successes, failures };
};

const failureSummary = ({ failures }: Pick<BulkIndexActionResult, 'failures'>) =>
  failures
    .slice(0, 3)
    .map(({ index, error }) => `${index.index_name}: ${extractErrorMessage(error)}`)
    .join('\n');

export const getBulkIndexActionNotification = (
  bulkAction: BulkIndexActionCandidate,
  result: BulkIndexActionResult,
): BulkIndexActionNotification => {
  const successCount = result.successes.length;
  const failureCount = result.failures.length;

  if (failureCount === 0) {
    return {
      type: 'success',
      message: bulkAction.successMessage(successCount),
    };
  }

  const failureDetails = failureSummary(result);
  const omittedFailures = failureCount > 3 ? `\n...and ${failureCount - 3} more.` : '';
  const message =
    successCount > 0
      ? `${successCount} succeeded, ${failureCount} failed.\n${failureDetails}${omittedFailures}`
      : `${failureCount} ${failureCount === 1 ? 'index' : 'indices'} failed.\n${failureDetails}${omittedFailures}`;

  return successCount > 0
    ? {
        type: 'warning',
        message,
        title: bulkAction.partialSuccessTitle,
      }
    : {
        type: 'error',
        message,
        title: bulkAction.failureTitle,
      };
};
