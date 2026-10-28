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
import React from 'react';
import { Trans } from 'react-i18next';

import { ClusterDeflector, IndexerIndices, SystemIndexerIndices } from '@graylog/server-api';

import type { StyleProps } from 'components/bootstrap/Button';
import type { IndexArchiveBinding } from 'components/indices/archive/types';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import i18n from 'i18n';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';

import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';

export type IndexAction = 'delete' | 'archive-delete' | 'reindex-system-index' | 'rotate';

export type ConfirmedAction = {
  action: IndexAction;
  index: IncompatibleIndex;
};

export type PendingArchiveTracking = {
  indexName: string;
  systemJobId: string;
};

type ActionDefinition = {
  buttonLabel: string;
  buttonStyle: StyleProps;
  confirmTitle: string;
  confirmText: string;
  errorTitle: string;
  confirmationBody: (index: IncompatibleIndex) => React.ReactNode;
  run: (index: IncompatibleIndex) => Promise<unknown>;
  successMessage: (index: IncompatibleIndex) => string;
  telemetryEventType: TelemetryEventType;
  getPendingArchiveTracking?: (index: IncompatibleIndex, response: unknown) => PendingArchiveTracking;
  isArchiveJobConflict?: (errorMessage: string) => boolean;
};

const deleteIncompatibleIndex = (index: IncompatibleIndex) =>
  index.managed_index ? IndexerIndices.remove(index.index_name) : SystemIndexerIndices.deleteOutdated(index.index_name);

const reindexSystemIndex = (index: IncompatibleIndex) => SystemIndexerIndices.reindex(index.index_name);

const rotateWriteIndex = (index: IncompatibleIndex) => ClusterDeflector.cycleByindexSetId(index.active_write_index);

const tr = (key: string, options: Record<string, unknown> = {}) => i18n.t(key, { ns: 'indexsets', ...options });

const boldIndexBody = (key: string, indexName: string) => (
  <p>
    <Trans i18nKey={key} ns="indexsets" values={{ indexName }} components={{ strong: <strong /> }} />
  </p>
);

export const getCoreActionDefinitions = (): Record<Exclude<IndexAction, 'archive-delete'>, ActionDefinition> => ({
  delete: {
    buttonLabel: tr('incompatible.delete.button'),
    buttonStyle: 'danger',
    confirmTitle: tr('incompatible.delete.title'),
    confirmText: tr('incompatible.delete.confirm'),
    errorTitle: tr('incompatible.delete.error'),
    confirmationBody: (index) => boldIndexBody('incompatible.delete.body', index.index_name),
    run: deleteIncompatibleIndex,
    successMessage: (index) => tr('incompatible.delete.success', { indexName: index.index_name }),
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
  },
  'reindex-system-index': {
    buttonLabel: tr('incompatible.reindex.button'),
    buttonStyle: 'primary',
    confirmTitle: tr('incompatible.reindex.title'),
    confirmText: tr('incompatible.reindex.confirm'),
    errorTitle: tr('incompatible.reindex.error'),
    confirmationBody: (index) => boldIndexBody('incompatible.reindex.body', index.index_name),
    run: reindexSystemIndex,
    successMessage: (index) => tr('incompatible.reindex.success', { indexName: index.index_name }),
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.SYSTEM_INDEX_REINDEX_CONFIRMED,
  },
  rotate: {
    buttonLabel: tr('incompatible.rotate.button'),
    buttonStyle: 'primary',
    confirmTitle: tr('incompatible.rotate.title'),
    confirmText: tr('incompatible.rotate.confirm'),
    errorTitle: tr('incompatible.rotate.error'),
    confirmationBody: (index) => (
      <div>
        {boldIndexBody('incompatible.rotate.body_active', index.index_name)}
        {boldIndexBody('incompatible.rotate.body_afterwards', index.index_name)}
      </div>
    ),
    run: rotateWriteIndex,
    successMessage: (index) => tr('incompatible.rotate.success', { indexName: index.index_name }),
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.WRITE_INDEX_ROTATE_CONFIRMED,
  },
});

const archiveDeleteDefinition = (archive: IndexArchiveBinding | undefined): ActionDefinition => ({
  buttonLabel: tr('incompatible.archive_delete.button'),
  buttonStyle: 'warning',
  confirmTitle: tr('incompatible.archive_delete.title'),
  confirmText: tr('incompatible.archive_delete.confirm'),
  errorTitle: tr('incompatible.archive_delete.error'),
  confirmationBody: (index) => boldIndexBody('incompatible.archive_delete.body', index.index_name),
  run: (index) =>
    archive
      ? archive.archiveAndDeleteIndex(index.index_name)
      : Promise.reject(new Error(tr('incompatible.archive_unavailable'))),
  successMessage: (index) => tr('incompatible.archive_delete.success', { indexName: index.index_name }),
  telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_ARCHIVE_AND_DELETE_CONFIRMED,
  getPendingArchiveTracking: (index, response) => ({
    indexName: index.index_name,
    systemJobId: (response as { systemJobId: string }).systemJobId,
  }),
  isArchiveJobConflict: (errorMessage) => archive?.isArchiveJobConflict(errorMessage) ?? false,
});

export const useIncompatibleIndexActionDefinitions = (): Record<IndexAction, ActionDefinition> => {
  const archive = useIndexArchive();

  return {
    ...getCoreActionDefinitions(),
    'archive-delete': archiveDeleteDefinition(archive),
  };
};

export const isIndexArchived = (
  indexName: string,
  pendingStatus: PendingIndexStatus | undefined,
  archivedIndexNames: ReadonlySet<string>,
): boolean => pendingStatus?.state !== 'archiving' && archivedIndexNames.has(indexName);

export type IndexActionAvailability = {
  canArchive: boolean;
  canReindex: boolean;
  alreadyArchived: boolean;
};

export const getAvailableActions = (
  index: IncompatibleIndex,
  { canArchive, canReindex, alreadyArchived }: IndexActionAvailability,
): Array<IndexAction> => {
  if (index.active_write_index) {
    return ['rotate'];
  }

  if (index.system_index) {
    return canReindex ? ['reindex-system-index'] : [];
  }

  return index.managed_index && canArchive && !alreadyArchived ? ['archive-delete', 'delete'] : ['delete'];
};
