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
import styled, { css } from 'styled-components';

import { Alert, SegmentedControl } from 'components/bootstrap';
import { ConfirmDialog, Spinner } from 'components/common';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import UserNotification from 'util/UserNotification';

import IndicesGroupTable from './IndicesGroupTable';
import { outdatedIndicesMockOverride } from './mockOutdatedIndices';
import { ACTION_DEFINITIONS } from './outdatedIndexActions';
import type { ConfirmedAction } from './outdatedIndexActions';
import { getFirstGroupWithIndices, getSelectedGroup, groupOutdatedIndices } from './outdatedIndexGroups';

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
  const actionDefinition = ACTION_DEFINITIONS[confirmedAction.action];

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

const OutdatedIndicesTable = () => {
  const { data: outdatedIndices, isError, isLoading, refetch } = useOutdatedIndices({
    mockData: outdatedIndicesMockOverride,
  });
  const canArchive = useCanArchive();
  const isUsingMockData = !!outdatedIndicesMockOverride;
  const [confirmedAction, setConfirmedAction] = useState<ConfirmedAction | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedGroupId, setSelectedGroupId] = useState<string | undefined>();

  const indicesGroups = useMemo(() => groupOutdatedIndices(outdatedIndices), [outdatedIndices]);
  const firstGroupWithIndices = useMemo(() => getFirstGroupWithIndices(indicesGroups), [indicesGroups]);
  const activeGroupId = selectedGroupId ?? firstGroupWithIndices;
  const selectedGroup = getSelectedGroup(indicesGroups, activeGroupId);
  const segments = useMemo(
    () => indicesGroups.map((group) => ({ value: group.id, label: `${group.shortLabel} (${group.indices.length})` })),
    [indicesGroups],
  );

  const closeConfirmDialog = () => setConfirmedAction(undefined);

  const handleConfirm = async () => {
    if (!confirmedAction) {
      return;
    }

    const actionDefinition = ACTION_DEFINITIONS[confirmedAction.action];

    setIsSubmitting(true);

    try {
      if (isUsingMockData) {
        UserNotification.success(`[Mock] ${actionDefinition.successMessage(confirmedAction.index)}`);
      } else {
        await actionDefinition.run(confirmedAction.index);
        UserNotification.success(actionDefinition.successMessage(confirmedAction.index));
        await refetch();
      }

      closeConfirmDialog();
    } catch (errorThrown) {
      UserNotification.error(String(errorThrown), `Could not ${actionDefinition.confirmText.toLowerCase()}.`);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <Spinner text="Loading outdated indices..." />;
  }

  if (isError) {
    return <Alert bsStyle="danger">Could not load outdated indices.</Alert>;
  }

  if (!outdatedIndices.length) {
    return <Alert bsStyle="success">No outdated indices found.</Alert>;
  }

  return (
    <>
      <Heading>Outdated indices</Heading>
      <SegmentedControl
        data={segments}
        value={activeGroupId}
        onChange={setSelectedGroupId}
        color="warning"
        autoContrast
      />
      <IndicesGroupTable group={selectedGroup} onAction={setConfirmedAction} canArchive={canArchive} />

      {confirmedAction && (
        <ActionConfirmDialog
          confirmedAction={confirmedAction}
          isSubmitting={isSubmitting}
          onCancel={closeConfirmDialog}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
};

export default OutdatedIndicesTable;
