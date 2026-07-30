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

import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import { getAvailableActions, isIndexArchived } from './incompatibleIndexActions';

const BULK_ACTION_ORDER = ['reindex-system-index', 'archive-delete', 'delete', 'rotate'] as const;

type BulkCapableIndexAction = (typeof BULK_ACTION_ORDER)[number];

type BulkActionCopy = {
  buttonLabel: string;
  confirmTitle: string;
  confirmText: string;
  targetVerb: string;
  successMessage: (count: number) => string;
};

export type BulkIndexActionCandidate = BulkActionCopy & {
  action: BulkCapableIndexAction;
  targetIndices: Array<IncompatibleIndex>;
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
  },
  'reindex-system-index': {
    buttonLabel: 'Reindex all',
    confirmTitle: 'Reindex system indices',
    confirmText: 'Reindex all',
    targetVerb: 'reindex',
    successMessage: (count) => `Reindex started for ${count} system ${count === 1 ? 'index' : 'indices'}.`,
  },
  'archive-delete': {
    buttonLabel: 'Archive and delete all',
    confirmTitle: 'Archive and delete indices',
    confirmText: 'Archive and delete all',
    targetVerb: 'archive and delete',
    successMessage: (count) => `Archive and delete started for ${count} ${count === 1 ? 'index' : 'indices'}.`,
  },
  rotate: {
    buttonLabel: 'Rotate all',
    confirmTitle: 'Rotate active write indices',
    confirmText: 'Rotate all',
    targetVerb: 'rotate',
    successMessage: (count) => `${count} ${count === 1 ? 'index was' : 'indices were'} rotated.`,
  },
};

export const buildPartialBulkNotification = ({
  succeeded,
  failures,
  successMessage,
  partialSuccessTitle,
  failureTitle,
}: {
  succeeded: number;
  failures: Array<{ name: string; explanation: string }>;
  successMessage: string;
  partialSuccessTitle: string;
  failureTitle: string;
}): BulkIndexActionNotification => {
  if (failures.length === 0) {
    return { type: 'success', message: successMessage };
  }

  const details = failures
    .slice(0, 3)
    .map(({ name, explanation }) => `${name}: ${explanation}`)
    .join('\n');
  const more = failures.length > 3 ? `\n...and ${failures.length - 3} more.` : '';
  const message =
    succeeded > 0
      ? `${succeeded} succeeded, ${failures.length} failed.\n${details}${more}`
      : `${failures.length} ${failures.length === 1 ? 'index' : 'indices'} failed.\n${details}${more}`;

  return succeeded > 0
    ? { type: 'warning', message, title: partialSuccessTitle }
    : { type: 'error', message, title: failureTitle };
};

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
        getAvailableActions(
          index,
          canArchive,
          isIndexArchived(index.index_name, pendingIndexStatuses.get(index.index_name), archivedIndexNames),
        ).includes(action) && !isArchiveInProgress(pendingIndexStatuses.get(index.index_name)),
    );

    return {
      action,
      targetIndices,
      ...BULK_ACTION_COPY[action],
    };
  }).filter((candidate) => candidate.targetIndices.length > 0);
