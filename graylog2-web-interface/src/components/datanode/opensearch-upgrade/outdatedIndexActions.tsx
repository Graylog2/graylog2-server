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

import { IndexerIndices } from '@graylog/server-api';

import type { StyleProps } from 'components/bootstrap/Button';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

export type IndexAction = 'delete' | 'archive-delete' | 'reindex-system-index';

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

const archiveAndDeleteIndex = (indexName: string) =>
  fetch(
    'POST',
    qualifyUrl(
      `/plugins/org.graylog.plugins.archive/cluster/archives/${encodeURIComponent(indexName)}?index_action=DELETE`,
    ),
  );

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
    run: (index) => IndexerIndices.remove(index.index_name),
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
    run: (index) => archiveAndDeleteIndex(index.index_name),
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
    run: (index) => IndexerIndices.reindex(index.index_name),
    successMessage: (index) => `Index "${index.index_name}" was reindexed.`,
    telemetryEventType: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.SYSTEM_INDEX_REINDEX_CONFIRMED,
  },
};

export const getAvailableActions = (index: OutdatedIndex, canArchive: boolean): Array<IndexAction> => {
  if (index.system_index) {
    return ['reindex-system-index'];
  }

  return index.managed_index && canArchive ? ['archive-delete', 'delete'] : ['delete'];
};
