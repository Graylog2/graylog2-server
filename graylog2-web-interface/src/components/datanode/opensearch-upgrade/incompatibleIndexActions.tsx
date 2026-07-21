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

import { ClusterDeflector, IndexerIndices } from '@graylog/server-api';

import type { StyleProps } from 'components/bootstrap/Button';
import type { IndexArchiveBinding } from 'components/indices/archive/types';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';

export type IndexAction = 'delete' | 'archive-delete' | 'reindex-system-index' | 'rotate';

export type ConfirmedAction = {
  action: IndexAction;
  index: IncompatibleIndex;
};

export type PendingArchiveTracking = {
  indexName: string;
  systemJobId?: string;
};

type ActionDefinition = {
  buttonLabel: string;
  buttonStyle: StyleProps;
  confirmTitle: string;
  confirmText: string;
  confirmationBody: (index: IncompatibleIndex) => React.ReactNode;
  run: (index: IncompatibleIndex) => Promise<unknown>;
  successMessage: (index: IncompatibleIndex) => string;
  telemetryEventType: TelemetryEventType;
  getPendingArchiveTracking?: (index: IncompatibleIndex, response: unknown) => PendingArchiveTracking;
  isArchiveJobConflict?: (errorMessage: string) => boolean;
};

const deleteIncompatibleIndex = (index: IncompatibleIndex) =>
  index.managed_index ? IndexerIndices.remove(index.index_name) : IndexerIndices.deleteOutdated(index.index_name);

const reindexSystemIndex = (index: IncompatibleIndex) => IndexerIndices.reindex(index.index_name);

const rotateWriteIndex = (index: IncompatibleIndex) => ClusterDeflector.cycleByindexSetId(index.active_write_index);

export const CORE_ACTION_DEFINITIONS: Record<Exclude<IndexAction, 'archive-delete'>, ActionDefinition> = {
  delete: {
    buttonLabel: 'Delete',
    buttonStyle: 'danger',
    confirmTitle: 'Delete index',
    confirmText: 'Delete',
    confirmationBody: (index) => (
      <p>
        This will permanently delete <strong>{index.index_name}</strong>.
      </p>
    ),
    run: deleteIncompatibleIndex,
    successMessage: (index) => `Index "${index.index_name}" was deleted.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
  },
  'reindex-system-index': {
    buttonLabel: 'Reindex',
    buttonStyle: 'primary',
    confirmTitle: 'Reindex system index',
    confirmText: 'Reindex system index',
    confirmationBody: (index) => (
      <p>
        This will reindex <strong>{index.index_name}</strong> so it can be used with OpenSearch 3.
      </p>
    ),
    run: reindexSystemIndex,
    successMessage: (index) => `Index "${index.index_name}" was reindexed.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.SYSTEM_INDEX_REINDEX_CONFIRMED,
  },
  rotate: {
    buttonLabel: 'Rotate',
    buttonStyle: 'primary',
    confirmTitle: 'Rotate active write index',
    confirmText: 'Rotate',
    confirmationBody: (index) => (
      <div>
        <p>
          <strong>{index.index_name}</strong> is the active write index of its index set and still receives new
          messages. Rotating starts a new write index on the current OpenSearch version.
        </p>
        <p>Afterwards, <strong>{index.index_name}</strong> can be archived or deleted.</p>
      </div>
    ),
    run: rotateWriteIndex,
    successMessage: (index) =>
      `The index set of "${index.index_name}" was rotated. The index no longer receives messages and can now be archived or deleted.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.WRITE_INDEX_ROTATE_CONFIRMED,
  },
};

const archiveDeleteDefinition = (archive: IndexArchiveBinding | undefined): ActionDefinition => ({
  buttonLabel: 'Archive and delete',
  buttonStyle: 'warning',
  confirmTitle: 'Archive and delete index',
  confirmText: 'Archive and delete',
  confirmationBody: (index) => (
    <p>
      This will create an archive for <strong>{index.index_name}</strong> and delete the index afterwards.
    </p>
  ),
  run: (index) =>
    archive ? archive.archiveAndDeleteIndex(index.index_name) : Promise.reject(new Error('Archiving is not available.')),
  successMessage: (index) => `Archive and delete job for "${index.index_name}" was started.`,
  telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_ARCHIVE_AND_DELETE_CONFIRMED,
  getPendingArchiveTracking: (index, response) => ({
    indexName: index.index_name,
    systemJobId: (response as { systemJobId?: string })?.systemJobId,
  }),
  isArchiveJobConflict: (errorMessage) => archive?.isArchiveJobConflict(errorMessage) ?? false,
});

export const useIncompatibleIndexActionDefinitions = (): Record<IndexAction, ActionDefinition> => {
  const archive = useIndexArchive();

  return {
    ...CORE_ACTION_DEFINITIONS,
    'archive-delete': archiveDeleteDefinition(archive),
  };
};

export const getAvailableActions = (
  index: IncompatibleIndex,
  canArchive: boolean,
  alreadyArchived: boolean,
): Array<IndexAction> => {
  if (index.active_write_index) {
    return ['rotate'];
  }

  if (index.system_index) {
    return ['reindex-system-index'];
  }

  return index.managed_index && canArchive && !alreadyArchived ? ['archive-delete', 'delete'] : ['delete'];
};
