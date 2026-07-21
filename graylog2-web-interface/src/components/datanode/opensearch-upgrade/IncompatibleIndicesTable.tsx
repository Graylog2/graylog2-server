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
import styled, { css } from 'styled-components';

import { Alert, Button, SegmentedControl } from 'components/bootstrap';
import { ConfirmDialog, Spinner } from 'components/common';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import extractErrorMessage from 'util/extractErrorMessage';
import UserNotification from 'util/UserNotification';

import BulkIndexActionConfirmDialog from './BulkIndexActionConfirmDialog';
import { getBulkIndexActionCandidates, getBulkIndexActionNotification, runBulkIndexAction } from './bulkIndexActions';
import type { BulkIndexActionCandidate, BulkIndexActionNotification } from './bulkIndexActions';
import IndicesGroupTable from './IndicesGroupTable';
import useArchivedIndexNames from './hooks/useArchivedIndexNames';
import usePendingIncompatibleIndexActions from './hooks/usePendingIncompatibleIndexActions';
import { useIncompatibleIndexActionDefinitions } from './incompatibleIndexActions';
import type { ConfirmedAction } from './incompatibleIndexActions';
import { getFirstGroupWithIndices, getSelectedGroup, groupIncompatibleIndices } from './incompatibleIndexGroups';

const TELEMETRY_DEFAULTS = { app_pathname: 'datanode', app_section: 'opensearch-upgrade' } as const;

const showBulkNotification = ({ type, message, title }: BulkIndexActionNotification) => {
  const notify = (notification: (notificationMessage: string, notificationTitle?: string) => void) =>
    title ? notification(message, title) : notification(message);

  if (type === 'success') {
    notify(UserNotification.success);
  } else if (type === 'warning') {
    notify(UserNotification.warning);
  } else {
    notify(UserNotification.error);
  }
};

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const ActionConfirmDialog = ({
  confirmedAction,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  confirmedAction: ConfirmedAction;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) => {
  const actionDefinitions = useIncompatibleIndexActionDefinitions();
  const actionDefinition = actionDefinitions[confirmedAction.action];

  return (
    <ConfirmDialog
      show
      title={actionDefinition.confirmTitle}
      btnConfirmText={actionDefinition.confirmText}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onConfirm={onConfirm}
      submitLoadingText="Working...">
      {actionDefinition.confirmationBody(confirmedAction.index)}
    </ConfirmDialog>
  );
};

const IncompatibleIndicesTable = () => {
  const { data: incompatibleIndices, isError, isLoading, refetch } = useIncompatibleIndices();
  const canArchive = useCanArchive();
  const actionDefinitions = useIncompatibleIndexActionDefinitions();
  const sendTelemetry = useSendTelemetry();
  const { pendingIndexStatuses, addArchiveDeleteAction, isArchiveJobRunning, refetchClusterJobs } =
    usePendingIncompatibleIndexActions({
      incompatibleIndices,
      isLoading,
      isError,
      refetch,
      canArchive,
    });
  const incompatibleIndexNames = incompatibleIndices.map((index) => index.index_name);
  const archivedIndexNames = useArchivedIndexNames(incompatibleIndexNames, canArchive);
  const [confirmedAction, setConfirmedAction] = useState<ConfirmedAction | undefined>();
  const [confirmedBulkAction, setConfirmedBulkAction] = useState<BulkIndexActionCandidate | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isBulkSubmitting, setIsBulkSubmitting] = useState(false);
  const [selectedGroupId, setSelectedGroupId] = useState<string | undefined>();

  const indicesGroups = groupIncompatibleIndices(incompatibleIndices);
  const firstGroupWithIndices = getFirstGroupWithIndices(indicesGroups);
  const activeGroupId = selectedGroupId ?? firstGroupWithIndices;
  const selectedGroup = getSelectedGroup(indicesGroups, activeGroupId);
  const archiveActionsAvailable = canArchive && !isArchiveJobRunning;
  const segments = indicesGroups.map((group) => ({
    value: group.id,
    label: `${group.shortLabel} (${group.indices.length})`,
  }));
  const bulkActions = getBulkIndexActionCandidates({
    indices: selectedGroup.indices,
    canArchive: archiveActionsAvailable,
    pendingIndexStatuses,
    archivedIndexNames,
  });

  const closeConfirmDialog = () => setConfirmedAction(undefined);
  const closeBulkConfirmDialog = () => setConfirmedBulkAction(undefined);
  const cancelBulkConfirmDialog = () => {
    if (!isBulkSubmitting) {
      closeBulkConfirmDialog();
    }
  };

  const finalizeAfterActions = async () => {
    const { data: updatedIncompatibleIndices = [] } = await refetch();
    const updatedGroups = groupIncompatibleIndices(updatedIncompatibleIndices);
    const updatedSelectedGroup = getSelectedGroup(updatedGroups, activeGroupId);

    if (updatedSelectedGroup.indices.length === 0) {
      setSelectedGroupId(getFirstGroupWithIndices(updatedGroups));
    }
  };

  const showArchiveJobConflictWarning = () => {
    UserNotification.warning(
      'Another archive job is already running. New archive jobs can be started after it finishes.',
      'Archive job already running',
    );
    refetchClusterJobs?.();
  };

  const handleConfirm = async () => {
    if (!confirmedAction) {
      return;
    }

    const { action, index } = confirmedAction;
    const actionDefinition = actionDefinitions[action];

    sendTelemetry(actionDefinition.telemetryEventType, { ...TELEMETRY_DEFAULTS });
    setIsSubmitting(true);

    try {
      const actionResponse = await actionDefinition.run(index);
      const pendingArchive = actionDefinition.getPendingArchiveTracking?.(index, actionResponse);

      if (pendingArchive) {
        addArchiveDeleteAction(pendingArchive);
      }

      UserNotification.success(actionDefinition.successMessage(index));
      await finalizeAfterActions();
      closeConfirmDialog();
    } catch (errorThrown) {
      const errorMessage = extractErrorMessage(errorThrown);

      if (actionDefinition.isArchiveJobConflict?.(errorMessage)) {
        showArchiveJobConflictWarning();
        closeConfirmDialog();
      } else {
        UserNotification.error(errorMessage, `Could not ${actionDefinition.confirmText.toLowerCase()}.`);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBulkConfirm = async () => {
    if (!confirmedBulkAction || isBulkSubmitting) {
      return;
    }

    sendTelemetry(actionDefinitions[confirmedBulkAction.action].telemetryEventType, {
      ...TELEMETRY_DEFAULTS,
      app_action_value: 'bulk',
      bulk_count: confirmedBulkAction.targetIndices.length,
    });
    setIsBulkSubmitting(true);

    try {
      const result = await runBulkIndexAction({
        action: confirmedBulkAction.action,
        indices: confirmedBulkAction.targetIndices,
      });

      showBulkNotification(getBulkIndexActionNotification(confirmedBulkAction, result));
      await finalizeAfterActions();
      closeBulkConfirmDialog();
    } catch (errorThrown) {
      UserNotification.error(
        extractErrorMessage(errorThrown),
        `Could not ${confirmedBulkAction.confirmText.toLowerCase()}.`,
      );
    } finally {
      setIsBulkSubmitting(false);
    }
  };

  if (isLoading) {
    return <Spinner text="Loading incompatible indices..." />;
  }

  if (isError) {
    return (
      <Alert bsStyle="danger">
        Could not load incompatible indices — retrying automatically.{' '}
        <Button bsSize="xs" onClick={() => refetch()}>
          Retry now
        </Button>
      </Alert>
    );
  }

  if (!incompatibleIndices.length) {
    return <Alert bsStyle="success">No incompatible indices found.</Alert>;
  }

  return (
    <>
      <Heading>Incompatible indices</Heading>
      <SegmentedControl
        data={segments}
        value={activeGroupId}
        onChange={setSelectedGroupId}
        color="warning"
        autoContrast
      />
      <IndicesGroupTable
        group={selectedGroup}
        onAction={setConfirmedAction}
        onBulkAction={setConfirmedBulkAction}
        canArchive={archiveActionsAvailable}
        pendingIndexStatuses={pendingIndexStatuses}
        archivedIndexNames={archivedIndexNames}
        bulkActions={bulkActions}
        isBulkActionSubmitting={isBulkSubmitting}
      />

      {confirmedAction && (
        <ActionConfirmDialog
          confirmedAction={confirmedAction}
          isSubmitting={isSubmitting}
          onCancel={closeConfirmDialog}
          onConfirm={handleConfirm}
        />
      )}
      {confirmedBulkAction && (
        <BulkIndexActionConfirmDialog
          bulkAction={confirmedBulkAction}
          isSubmitting={isBulkSubmitting}
          onCancel={cancelBulkConfirmDialog}
          onConfirm={handleBulkConfirm}
        />
      )}
    </>
  );
};

export default IncompatibleIndicesTable;
