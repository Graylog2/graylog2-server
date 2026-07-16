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
import React, { useMemo, useState } from 'react';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import UserNotification from 'util/UserNotification';

import { TELEMETRY_DEFAULTS } from './telemetry';
import BulkIndexActionConfirmDialog from './BulkIndexActionConfirmDialog';
import { CORE_ACTION_DEFINITIONS } from './incompatibleIndexActions';
import {
  getBulkIndexActionCandidates,
  getBulkIndexActionNotification,
  runBulkIndexAction,
} from './bulkIndexActions';
import type { BulkIndexActionCandidate } from './bulkIndexActions';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';

type Props = {
  indices: Array<IncompatibleIndexRow>;
  canArchive: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: ReadonlySet<string>;
  refetch: () => void;
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

  const selectedIndices = useMemo(
    () => indices.filter((index) => selectedEntities.includes(index.id)),
    [indices, selectedEntities],
  );

  const candidates = useMemo(
    () => getBulkIndexActionCandidates({ indices: selectedIndices, canArchive, pendingIndexStatuses, archivedIndexNames }),
    [selectedIndices, canArchive, pendingIndexStatuses, archivedIndexNames],
  );

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
      const result = await runBulkIndexAction({
        action: confirmedBulkAction.action,
        indices: confirmedBulkAction.targetIndices,
      });

      const notification = getBulkIndexActionNotification(confirmedBulkAction, result);

      if (notification.type === 'success') {
        UserNotification.success(notification.message);
      } else if (notification.type === 'warning') {
        UserNotification.warning(notification.message, notification.title);
      } else {
        UserNotification.error(notification.message, notification.title);
      }

      const succeededIds = new Set(result.successes.map(({ index }) => index.index_name));
      setSelectedEntities(selectedEntities.filter((id) => !succeededIds.has(id)));
      refetch();
      setConfirmedBulkAction(undefined);
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
          onCancel={() => setConfirmedBulkAction(undefined)}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
};

export default IncompatibleIndicesBulkActions;
