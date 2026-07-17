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
import styled from 'styled-components';

import { Button, ButtonToolbar, Label } from 'components/bootstrap';
import { ConfirmDialog, ProgressBar } from 'components/common';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import { TELEMETRY_DEFAULTS } from './telemetry';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import { getAvailableActions, useIncompatibleIndexActionDefinitions } from './incompatibleIndexActions';
import type { IndexAction, PendingArchiveTracking } from './incompatibleIndexActions';

const ActionsToolbar = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const ArchiveProgressBar = styled(ProgressBar)`
  display: inline-flex;
  width: 120px;
  margin-bottom: 0;
  vertical-align: middle;
`;

type Props = {
  index: IncompatibleIndexRow;
  canArchive: boolean;
  pendingStatus: PendingIndexStatus | undefined;
  isArchived: boolean;
  addArchiveDeleteAction: (tracking: PendingArchiveTracking) => void;
  refetchClusterJobs?: () => void;
  refetch: () => void;
};

const IncompatibleIndexTableActions = ({
  index,
  canArchive,
  pendingStatus,
  isArchived,
  addArchiveDeleteAction,
  refetchClusterJobs = undefined,
  refetch,
}: Props) => {
  const actionDefinitions = useIncompatibleIndexActionDefinitions();
  const sendTelemetry = useSendTelemetry();
  const { deselectEntity } = useSelectedEntities();
  const [confirmedAction, setConfirmedAction] = useState<IndexAction | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (pendingStatus?.state === 'archiving') {
    return pendingStatus.percent > 0 ? (
      <ArchiveProgressBar
        bars={[
          {
            value: pendingStatus.percent,
            label: `${pendingStatus.percent}%`,
            bsStyle: 'warning',
            animated: true,
            striped: true,
          },
        ]}
      />
    ) : (
      <Label bsStyle="warning">Archiving...</Label>
    );
  }

  const actions = getAvailableActions(index, canArchive, isArchived);

  const handleConfirm = async () => {
    if (!confirmedAction) {
      return;
    }

    const actionDefinition = actionDefinitions[confirmedAction];
    sendTelemetry(actionDefinition.telemetryEventType, { ...TELEMETRY_DEFAULTS });
    setIsSubmitting(true);

    try {
      const actionResponse = await actionDefinition.run(index);
      const pendingArchive = actionDefinition.getPendingArchiveTracking?.(index, actionResponse);

      if (pendingArchive) {
        addArchiveDeleteAction(pendingArchive);
      }

      UserNotification.success(actionDefinition.successMessage(index));

      if (confirmedAction === 'delete') {
        deselectEntity(index.id);
      }

      refetch();
      setConfirmedAction(undefined);
    } catch (errorThrown) {
      const errorMessage = extractErrorMessage(errorThrown);

      if (actionDefinition.isArchiveJobConflict?.(errorMessage)) {
        UserNotification.warning(
          'Another archive job is already running. New archive jobs can be started after it finishes.',
          'Archive job already running',
        );
        refetchClusterJobs?.();
        setConfirmedAction(undefined);
      } else {
        UserNotification.error(errorMessage, `Could not ${actionDefinition.confirmText.toLowerCase()}.`);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const confirmDefinition = confirmedAction ? actionDefinitions[confirmedAction] : undefined;

  return (
    <ActionsToolbar>
      {pendingStatus?.state === 'failed' && (
        <Label bsStyle="danger" title={pendingStatus.message}>
          Archive failed
        </Label>
      )}
      {actions.map((action) => {
        const actionDefinition = actionDefinitions[action];

        return (
          <Button
            key={action}
            bsSize="xs"
            bsStyle={actionDefinition.buttonStyle}
            onClick={() => setConfirmedAction(action)}>
            {actionDefinition.buttonLabel}
          </Button>
        );
      })}
      {confirmDefinition && (
        <ConfirmDialog
          show
          title={confirmDefinition.confirmTitle}
          btnConfirmText={confirmDefinition.confirmText}
          isAsyncSubmit
          isSubmitting={isSubmitting}
          onCancel={() => setConfirmedAction(undefined)}
          onConfirm={handleConfirm}
          submitLoadingText="Working...">
          {confirmDefinition.confirmationBody(index)}
        </ConfirmDialog>
      )}
    </ActionsToolbar>
  );
};

export default IncompatibleIndexTableActions;
