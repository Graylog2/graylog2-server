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
import React, { useState } from 'react';

import { ClusterDeflector, SystemIndexerIndices } from '@graylog/server-api';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IndexArchiveBinding } from 'components/indices/archive/types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import { TELEMETRY_DEFAULTS } from './telemetry';
import BulkIndexActionConfirmDialog from './BulkIndexActionConfirmDialog';
import type { PendingArchiveTracking } from './incompatibleIndexActions';
import { useIncompatibleIndicesContext } from './IncompatibleIndicesContext';
import { buildPartialBulkNotification, getBulkIndexActionCandidates } from './bulkIndexActions';
import type { BulkIndexActionCandidate, BulkIndexActionNotification } from './bulkIndexActions';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';

type Props = {
  indices: Array<IncompatibleIndexRow>;
};

const BULK_ACTION_TELEMETRY: Record<BulkIndexActionCandidate['action'], TelemetryEventType> = {
  delete: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
  'reindex-system-index': TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.SYSTEM_INDEX_REINDEX_CONFIRMED,
  'archive-delete': TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_ARCHIVE_AND_DELETE_CONFIRMED,
  rotate: TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.WRITE_INDEX_ROTATE_CONFIRMED,
};

const showNotification = ({ type, message, title }: BulkIndexActionNotification) => {
  if (type === 'success') {
    UserNotification.success(message);
  } else if (type === 'warning') {
    UserNotification.warning(message, title);
  } else {
    UserNotification.error(message, title);
  }
};

const bulkDeleteIndices = async (bulkAction: BulkIndexActionCandidate): Promise<Array<string>> => {
  const entityIds = bulkAction.targetIndices.map((index) => index.index_name);
  const { failures } = await SystemIndexerIndices.bulkDeleteOutdated({ entity_ids: entityIds });
  const failedIds = new Set((failures ?? []).map(({ entity_id }) => entity_id));
  const succeeded = entityIds.length - failedIds.size;

  showNotification(
    buildPartialBulkNotification({
      succeeded,
      failures: (failures ?? []).map(({ entity_id, failure_explanation }) => ({
        name: entity_id,
        explanation: failure_explanation,
      })),
      successMessage: `${succeeded} ${succeeded === 1 ? 'index was' : 'indices were'} deleted.`,
      partialSuccessTitle: 'Some indices could not be deleted',
      failureTitle: 'Could not delete indices',
    }),
  );

  return entityIds.filter((id) => !failedIds.has(id));
};

const bulkReindexIndices = async (
  bulkAction: BulkIndexActionCandidate,
  addReindexAction: (tracking: { indexName: string }) => void,
): Promise<Array<string>> => {
  const indexNames = bulkAction.targetIndices.map((index) => index.index_name);
  await SystemIndexerIndices.bulkReindex({ indices: indexNames, with_replication: true });
  indexNames.forEach((indexName) => addReindexAction({ indexName }));
  showNotification({ type: 'success', message: bulkAction.successMessage(indexNames.length) });

  return indexNames;
};

const bulkRotateIndices = async (bulkAction: BulkIndexActionCandidate): Promise<Array<string>> => {
  const namesByIndexSet = new Map<string, Array<string>>();
  bulkAction.targetIndices.forEach((index) => {
    const names = namesByIndexSet.get(index.active_write_index) ?? [];
    names.push(index.index_name);
    namesByIndexSet.set(index.active_write_index, names);
  });

  const { success, entity, error_text } = await ClusterDeflector.bulkcycle({
    entity_ids: Array.from(namesByIndexSet.keys()),
  });

  if (!success) {
    throw new Error(error_text || 'Bulk rotation request failed.');
  }

  const explanationByIndexSet = new Map((entity?.failures ?? []).map((f) => [f.entity_id, f.failure_explanation]));
  const succeededNames: Array<string> = [];
  const failures: Array<{ name: string; explanation: string }> = [];
  namesByIndexSet.forEach((names, indexSetId) => {
    const explanation = explanationByIndexSet.get(indexSetId);

    if (explanation !== undefined) {
      names.forEach((name) => failures.push({ name, explanation }));
    } else {
      succeededNames.push(...names);
    }
  });

  showNotification(
    buildPartialBulkNotification({
      succeeded: succeededNames.length,
      failures,
      successMessage: `${succeededNames.length} ${succeededNames.length === 1 ? 'index was' : 'indices were'} rotated.`,
      partialSuccessTitle: 'Some indices could not be rotated',
      failureTitle: 'Could not rotate indices',
    }),
  );

  return succeededNames;
};

const bulkArchiveDeleteIndices = async (
  bulkAction: BulkIndexActionCandidate,
  archive: IndexArchiveBinding | undefined,
  addArchiveDeleteAction: (tracking: PendingArchiveTracking) => void,
): Promise<Array<string>> => {
  if (!archive) {
    throw new Error('Archiving is not available.');
  }

  const indexNames = bulkAction.targetIndices.map((index) => index.index_name);
  const { systemJobId } = await archive.archiveAndDeleteIndices(indexNames);
  indexNames.forEach((indexName) => addArchiveDeleteAction({ indexName, systemJobId }));
  showNotification({ type: 'success', message: bulkAction.successMessage(indexNames.length) });

  return indexNames;
};

const IncompatibleIndicesBulkActions = ({ indices }: Props) => {
  const {
    archiveActionsAvailable,
    reindexActionsAvailable,
    archivedIndexNames,
    pendingIndexStatuses,
    addArchiveDeleteAction,
    addReindexAction,
    refetchClusterJobs,
    refetch,
  } = useIncompatibleIndicesContext();
  const sendTelemetry = useSendTelemetry();
  const archive = useIndexArchive();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [confirmedBulkAction, setConfirmedBulkAction] = useState<BulkIndexActionCandidate | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const candidates = getBulkIndexActionCandidates({
    indices,
    canArchive: archiveActionsAvailable,
    canReindex: reindexActionsAvailable,
    pendingIndexStatuses,
    archivedIndexNames,
  });

  const handleCancel = () => {
    if (!isSubmitting) {
      setConfirmedBulkAction(undefined);
    }
  };

  const handleConfirm = async () => {
    if (!confirmedBulkAction || isSubmitting) {
      return;
    }

    sendTelemetry(BULK_ACTION_TELEMETRY[confirmedBulkAction.action], {
      ...TELEMETRY_DEFAULTS,
      app_action_value: 'bulk',
      bulk_count: confirmedBulkAction.targetIndices.length,
    });
    setIsSubmitting(true);

    try {
      let succeededIds: Array<string>;

      switch (confirmedBulkAction.action) {
        case 'delete':
          succeededIds = await bulkDeleteIndices(confirmedBulkAction);
          break;
        case 'reindex-system-index':
          succeededIds = await bulkReindexIndices(confirmedBulkAction, addReindexAction);
          break;
        case 'rotate':
          succeededIds = await bulkRotateIndices(confirmedBulkAction);
          break;
        default:
          succeededIds = await bulkArchiveDeleteIndices(confirmedBulkAction, archive, addArchiveDeleteAction);
          break;
      }

      setSelectedEntities(selectedEntities.filter((id) => !succeededIds.includes(id)));
      refetch();

      if (confirmedBulkAction.action === 'archive-delete') {
        refetchClusterJobs?.();
      }

      setConfirmedBulkAction(undefined);
    } catch (errorThrown) {
      const errorMessage = extractErrorMessage(errorThrown);

      if (confirmedBulkAction.action === 'archive-delete' && archive?.isArchiveJobConflict(errorMessage)) {
        UserNotification.warning(
          'Another archive job is already running. New archive jobs can be started after it finishes.',
          'Archive job already running',
        );
        refetchClusterJobs?.();
        setConfirmedBulkAction(undefined);
      } else {
        UserNotification.error(errorMessage, `Could not ${confirmedBulkAction.confirmText.toLowerCase()}.`);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <BulkActionsDropdown>
        {candidates.map((candidate) => (
          <MenuItem key={candidate.action} onSelect={() => setConfirmedBulkAction(candidate)}>
            {candidate.buttonLabel}
          </MenuItem>
        ))}
      </BulkActionsDropdown>
      {confirmedBulkAction && (
        <BulkIndexActionConfirmDialog
          bulkAction={confirmedBulkAction}
          isSubmitting={isSubmitting}
          onCancel={handleCancel}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
};

export default IncompatibleIndicesBulkActions;
