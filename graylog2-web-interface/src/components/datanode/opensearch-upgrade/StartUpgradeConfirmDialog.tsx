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
import styled, { css } from 'styled-components';

import { Alert } from 'components/bootstrap';
import { ConfirmDialog } from 'components/common';

const Paragraph = styled.p(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};

    &:last-child {
      margin-bottom: 0;
    }
  `,
);

type Props = {
  currentVersion: string | undefined;
  targetVersion: string | undefined;
  numberOfDataNodes: number;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

const versionSummary = (currentVersion: string | undefined, targetVersion: string | undefined) =>
  currentVersion && targetVersion ? (
    <>
      from <strong>{currentVersion}</strong> to <strong>{targetVersion}</strong>
    </>
  ) : (
    <>to the target version</>
  );

const StartUpgradeConfirmDialog = ({
  currentVersion,
  targetVersion,
  numberOfDataNodes,
  isSubmitting,
  onCancel,
  onConfirm,
}: Props) => (
  <ConfirmDialog
    show
    title="Start OpenSearch rolling upgrade?"
    btnConfirmText="Start rolling upgrade"
    isAsyncSubmit
    isSubmitting={isSubmitting}
    onCancel={onCancel}
    onConfirm={onConfirm}
    submitLoadingText="Starting...">
    <Alert bsStyle="warning" title="This restarts every Data Node, one at a time">
      <Paragraph>
        Each of your {numberOfDataNodes} Data Nodes will be upgraded and restarted one at a time to move its embedded
        OpenSearch {versionSummary(currentVersion, targetVersion)}.
      </Paragraph>
      <Paragraph>
        Shard allocation is paused during each restart, so the cluster stays available — but indexing and search may
        be briefly degraded while a node leaves and rejoins.
      </Paragraph>
      <Paragraph>
        After each node the upgrade waits for the cluster to return to green before continuing. If that takes too
        long it pauses and waits for you to resume.
      </Paragraph>
      <Paragraph>Once started, it cannot be rolled back to the previous version.</Paragraph>
    </Alert>
    <p>Make sure the cluster is healthy (green) before proceeding.</p>
  </ConfirmDialog>
);

export default StartUpgradeConfirmDialog;
