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

import { SystemIndexerIndices } from '@graylog/server-api';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import { TELEMETRY_DEFAULTS } from './telemetry';
import BulkIndexActionConfirmDialog from './BulkIndexActionConfirmDialog';
import { CORE_ACTION_DEFINITIONS } from './incompatibleIndexActions';
import { getBulkIndexActionCandidates, getBulkIndexActionNotification, runBulkIndexAction } from './bulkIndexActions';
import type { BulkIndexActionCandidate, BulkIndexActionNotification } from './bulkIndexActions';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';

type Props = {
  indices: Array<IncompatibleIndexRow>;
  canArchive: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: ReadonlySet<string>;
  refetch: () => void;
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
  const failedIds = (failures ?? []).map(({ entity_id }) => entity_id);
  const succeeded = entityIds.length - failedIds.length;

  if (failedIds.length === 0) {
    showNotification({
      type: 'success',
      message: `${succeeded} ${succeeded === 1 ? 'index was' : 'indices were'} deleted.`,
    });
  } else {
    const details = (failures ?? [])
      .slice(0, 3)
      .map(({ entity_id, failure_explanation }) => `${entity_id}: ${failure_explanation}`)
      .join('\n');
    const more = failedIds.length > 3 ? `\n...and ${failedIds.length - 3} more.` : '';
    const message =
      succeeded > 0
        ? `${succeeded} succeeded, ${failedIds.length} failed.\n${details}${more}`
        : `${failedIds.length} ${failedIds.length === 1 ? 'index' : 'indices'} failed.\n${details}${more}`;

    showNotification(
      succeeded > 0
        ? { type: 'warning', message, title: 'Some indices could not be deleted' }
        : { type: 'error', message, title: 'Could not delete indices' },
    );
  }

  return entityIds.filter((id) => !failedIds.includes(id));
};

const bulkReindexIndices = async (bulkAction: BulkIndexActionCandidate): Promise<Array<string>> => {
  const result = await runBulkIndexAction({ action: bulkAction.action, indices: bulkAction.targetIndices });
  showNotification(getBulkIndexActionNotification(bulkAction, result));

  return result.successes.map(({ index }) => index.index_name);
};

const IncompatibleIndicesBulkActions = ({
  indices,
  canArchive,
  pendingIndexStatuses,
  archivedIndexNames,
  refetch,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [confirmedBulkAction, setConfirmedBulkAction] = useState<BulkIndexActionCandidate | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedIndices = indices.filter((index) => selectedEntities.includes(index.id));
  const candidates = getBulkIndexActionCandidates({
    indices: selectedIndices,
    canArchive,
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

    sendTelemetry(CORE_ACTION_DEFINITIONS[confirmedBulkAction.action].telemetryEventType, {
      ...TELEMETRY_DEFAULTS,
      app_action_value: 'bulk',
      bulk_count: confirmedBulkAction.targetIndices.length,
    });
    setIsSubmitting(true);

    try {
      const succeededIds =
        confirmedBulkAction.action === 'delete'
          ? await bulkDeleteIndices(confirmedBulkAction)
          : await bulkReindexIndices(confirmedBulkAction);

      setSelectedEntities(selectedEntities.filter((id) => !succeededIds.includes(id)));
      refetch();
      setConfirmedBulkAction(undefined);
    } catch (errorThrown) {
      UserNotification.error(
        extractErrorMessage(errorThrown),
        `Could not ${confirmedBulkAction.confirmText.toLowerCase()}.`,
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <BulkActionsDropdown>
        {candidates.map((candidate) => (
          <MenuItem key={candidate.action} onSelect={() => setConfirmedBulkAction(candidate)}>
            {candidate.buttonLabel} ({candidate.targetIndices.length})
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
