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
import i18n from 'i18n';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import { getAvailableActions, isIndexArchived } from './incompatibleIndexActions';

const BULK_ACTION_ORDER = ['reindex-system-index', 'archive-delete', 'delete', 'rotate'] as const;

type BulkCapableIndexAction = (typeof BULK_ACTION_ORDER)[number];

type BulkActionCopy = {
  buttonLabel: string;
  confirmTitle: string;
  confirmText: string;
  confirmBody: (count: number) => string;
  errorTitle: string;
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

const bulkActionKey = (action: BulkCapableIndexAction) => action.replace('-system-index', '').replace('-', '_');

const getBulkActionCopy = (action: BulkCapableIndexAction): BulkActionCopy => {
  const key = `incompatible.bulk.${bulkActionKey(action)}`;
  const translate = (suffix: string, options: Record<string, unknown> = {}) =>
    i18n.t(`${key}.${suffix}`, { ns: 'indexsets', ...options });

  return {
    buttonLabel: translate('button'),
    confirmTitle: translate('title'),
    confirmText: translate('confirm'),
    confirmBody: (count) => translate('body', { count }),
    errorTitle: translate('error'),
    successMessage: (count) => translate('success', { count }),
  };
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
  const more =
    failures.length > 3
      ? `\n${i18n.t('incompatible.partial_more', { ns: 'indexsets', count: failures.length - 3 })}`
      : '';
  const summary =
    succeeded > 0
      ? i18n.t('incompatible.partial_summary', { ns: 'indexsets', succeeded, failed: failures.length })
      : i18n.t('incompatible.all_failed', { ns: 'indexsets', count: failures.length });
  const message = `${summary}\n${details}${more}`;

  return succeeded > 0
    ? { type: 'warning', message, title: partialSuccessTitle }
    : { type: 'error', message, title: failureTitle };
};

const isActionInProgress = (pendingStatus: PendingIndexStatus | undefined) =>
  pendingStatus?.state === 'archiving' || pendingStatus?.state === 'reindexing';

export const getBulkIndexActionCandidates = ({
  indices,
  canArchive,
  canReindex,
  pendingIndexStatuses,
  archivedIndexNames,
}: {
  indices: Array<IncompatibleIndex>;
  canArchive: boolean;
  canReindex: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: ReadonlySet<string>;
}): Array<BulkIndexActionCandidate> =>
  BULK_ACTION_ORDER.map((action) => {
    const targetIndices = indices.filter(
      (index) =>
        getAvailableActions(index, {
          canArchive,
          canReindex,
          alreadyArchived: isIndexArchived(
            index.index_name,
            pendingIndexStatuses.get(index.index_name),
            archivedIndexNames,
          ),
        }).includes(action) && !isActionInProgress(pendingIndexStatuses.get(index.index_name)),
    );

    return {
      action,
      targetIndices,
      ...getBulkActionCopy(action),
    };
  }).filter((candidate) => candidate.targetIndices.length > 0);
