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
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

export type IndexAction = 'delete' | 'archive-delete' | 'reindex-system-index' | 'rotate';

export type ConfirmedAction = {
  action: IndexAction;
  index: OutdatedIndex;
};

type ActionDefinition = {
  buttonLabel: string;
  buttonStyle: StyleProps;
  confirmTitle: string;
  confirmText: string;
  confirmationBody: (index: OutdatedIndex) => React.ReactNode;
  run: (index: OutdatedIndex) => Promise<unknown>;
  successMessage: (index: OutdatedIndex) => string;
  telemetryEventType: TelemetryEventType;
};

// Archiving lives in the enterprise Archive plugin. Its `@graylog/server-api` stub only exists after a local
// enterprise-stub sync; CI generates core stubs only, so importing it there breaks the build. Call the
// endpoint directly to stay CI-safe.
const archiveAndDeleteIndex = (index: OutdatedIndex) =>
  fetch(
    'POST',
    qualifyUrl(
      `/plugins/org.graylog.plugins.archive/cluster/archives/${encodeURIComponent(index.index_name)}?index_action=DELETE`,
    ),
  );

const deleteOutdatedIndex = (index: OutdatedIndex) =>
  index.managed_index ? IndexerIndices.remove(index.index_name) : IndexerIndices.deleteOutdated(index.index_name);

const reindexSystemIndex = (index: OutdatedIndex) => IndexerIndices.reindex(index.index_name);

const rotateWriteIndex = (index: OutdatedIndex) => ClusterDeflector.cycleByindexSetId(index.active_write_index);

export const ACTION_DEFINITIONS: Record<IndexAction, ActionDefinition> = {
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
    run: deleteOutdatedIndex,
    successMessage: (index) => `Index "${index.index_name}" was deleted.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
  },
  'archive-delete': {
    buttonLabel: 'Archive and delete',
    buttonStyle: 'warning',
    confirmTitle: 'Archive and delete index',
    confirmText: 'Archive and delete',
    confirmationBody: (index) => (
      <p>
        This will create an archive for <strong>{index.index_name}</strong> and delete the index afterwards.
      </p>
    ),
    run: archiveAndDeleteIndex,
    successMessage: (index) => `Archive and delete job for "${index.index_name}" was started.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_ARCHIVE_AND_DELETE_CONFIRMED,
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

export const getAvailableActions = (
  index: OutdatedIndex,
  canArchive: boolean,
  alreadyArchived: boolean,
): Array<IndexAction> => {
  // The active write index still receives messages: deleting it breaks ingestion and reindexing it races
  // incoming writes. It has to be rotated out of write duty first — everything else unlocks afterwards.
  if (index.active_write_index) {
    return ['rotate'];
  }

  if (index.system_index) {
    return ['reindex-system-index'];
  }

  // An index that already has an archive should not be archived again (e.g. the "archived but delete skipped"
  // case, where a completed archive left the index in place) — offer a plain delete instead.
  return index.managed_index && canArchive && !alreadyArchived ? ['archive-delete', 'delete'] : ['delete'];
};
