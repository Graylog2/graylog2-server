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
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import extractErrorMessage from 'util/extractErrorMessage';

import { BULK_INDEX_ACTION_CONCURRENCY } from './constants';
import type { PendingIndexStatus } from './hooks/usePendingOutdatedIndexActions';
import { ACTION_DEFINITIONS, getAvailableActions } from './outdatedIndexActions';
import type { IndexAction } from './outdatedIndexActions';

// Archive creation is backed by a single-concurrency system job. Offering a frontend bulk action made from
// multiple single-index requests races against that backend limit, so keep archive/delete as a row action only.
// Rotate is row-only by nature: it targets the index set of one active write index, not the index itself.
type BulkCapableIndexAction = Exclude<IndexAction, 'rotate'>;

const BULK_ACTION_ORDER: Array<BulkCapableIndexAction> = ['delete', 'reindex-system-index'];

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
  targetIndices: Array<OutdatedIndex>;
};

export type BulkIndexActionSuccess = {
  index: OutdatedIndex;
  response: unknown;
};

export type BulkIndexActionFailure = {
  index: OutdatedIndex;
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
    confirmTitle: 'Delete outdated indices',
    confirmText: 'Delete all',
    targetVerb: 'delete',
    successMessage: (count) => `${count} outdated ${count === 1 ? 'index was' : 'indices were'} deleted.`,
    partialSuccessTitle: 'Some indices could not be deleted',
    failureTitle: 'Could not delete indices',
  },
  'archive-delete': {
    buttonLabel: 'Archive and delete all',
    confirmTitle: 'Archive and delete outdated indices',
    confirmText: 'Archive and delete all',
    targetVerb: 'archive and delete',
    successMessage: (count) =>
      `Archive and delete ${count === 1 ? 'job was' : 'jobs were'} started for ${count} outdated ${
        count === 1 ? 'index' : 'indices'
      }.`,
    partialSuccessTitle: 'Some archive and delete jobs could not be started',
    failureTitle: 'Could not start archive and delete jobs',
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

// Only an in-flight archive blocks bulk delete — deleting mid-archive is racy. An already-archived index
// ("delete skipped") is deletable, just like its per-row Delete action, so it stays a bulk delete candidate.
const isArchiveInProgress = (pendingStatus: PendingIndexStatus | undefined) => pendingStatus?.state === 'archiving';

export const getBulkIndexActionCandidates = ({
  indices,
  canArchive,
  pendingIndexStatuses,
  archivedIndexNames,
}: {
  indices: Array<OutdatedIndex>;
  canArchive: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: Set<string>;
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

// Each index is processed via its own request. If one 403s — e.g. deleting an active write index —
// FetchProvider's global "Missing Permissions" handler redirects the whole page, which the per-index
// catch below cannot intercept, aborting the rest of the batch. An active write index never reaches a
// batch though: getAvailableActions only offers it the row-level rotate action, keeping it out of every
// bulk candidate list.
export const runBulkIndexAction = async ({
  action,
  indices,
  onIndexSuccess,
}: {
  action: IndexAction;
  indices: Array<OutdatedIndex>;
  onIndexSuccess?: (success: BulkIndexActionSuccess) => void;
}): Promise<BulkIndexActionResult> => {
  const actionDefinition = ACTION_DEFINITIONS[action];
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
        const success = { index, response };

        successes.push(success);
        onIndexSuccess?.(success);
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
