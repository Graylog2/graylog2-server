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

import { Alert, Col, Label, Row } from 'components/bootstrap';
import { Icon } from 'components/common';

import type { OpenSearchVersionNode } from './hooks/useOpenSearchClusterStats';
import OpenSearchRollingUpgradeNodeTable from './OpenSearchRollingUpgradeNodeTable';
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';
import { isRestartOnly, isRollingRestartPaused, isRollingRestartTerminalState } from './rollingRestartTypes';

type Props = {
  job: RollingRestartJob | null | undefined;
  versionNodes: Array<OpenSearchVersionNode>;
};

const SectionHeading = styled.h2(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
    margin-bottom: ${theme.spacings.md};
  `,
);

const NodesHeading = styled.h3(
  ({ theme }) => css`
    margin-top: 0;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const ProgressSpinner = styled(Icon)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xxs};
  `,
);

const STATE_LABELS: Record<RollingRestartState, string> = {
  PREPARING_CLUSTER: 'Preparing cluster',
  SELECTING_NEXT_NODE: 'Selecting next node',
  UPGRADING_NODE: 'Upgrading node',
  WAITING_NODE_JOINED: 'Waiting for node to rejoin',
  REENABLING_ALLOCATION: 'Re-enabling allocation',
  WAITING_GREEN: 'Waiting for green cluster',
  PAUSED_WAITING_GREEN: 'Paused waiting for green cluster',
  FINALIZING: 'Finalizing',
  COMPLETED: 'Completed',
  ABORTED: 'Aborted',
  FAILED: 'Failed',
};

const UPGRADE_TEXTS = {
  heading: 'OpenSearch rolling upgrade',
  abortRequested: 'Abort requested. The upgrade will stop after the current step.',
  remainingHeading: 'Waiting for OpenSearch upgrade',
  completedHeading: 'Upgraded to target OpenSearch',
  noCompletedNodes: 'No upgraded nodes yet.',
};

const RESTART_TEXTS: typeof UPGRADE_TEXTS = {
  heading: 'Data Node rolling restart',
  abortRequested: 'Abort requested. The restart will stop after the current step.',
  remainingHeading: 'Waiting for restart',
  completedHeading: 'Restarted',
  noCompletedNodes: 'No restarted nodes yet.',
};

const STATE_STYLE: Record<RollingRestartState, 'default' | 'info' | 'success' | 'warning' | 'danger'> = {
  PREPARING_CLUSTER: 'info',
  SELECTING_NEXT_NODE: 'info',
  UPGRADING_NODE: 'info',
  WAITING_NODE_JOINED: 'info',
  REENABLING_ALLOCATION: 'info',
  WAITING_GREEN: 'info',
  PAUSED_WAITING_GREEN: 'warning',
  FINALIZING: 'info',
  COMPLETED: 'success',
  ABORTED: 'warning',
  FAILED: 'danger',
};

const OpenSearchRollingUpgradeNodes = ({ job, versionNodes }: Props) => {
  const data = job?.data;

  if (!data) {
    return <Alert bsStyle="info">No OpenSearch rolling upgrade has been started yet.</Alert>;
  }

  const versionNodeById = new Map(versionNodes.map((versionNode) => [versionNode.node_id, versionNode]));
  const nodes = data.nodes.map((node, index) => ({
    node,
    index,
    versionNode: versionNodeById.get(node.datanode_id),
  }));
  const completedNodes = nodes.filter(({ node }) => node.status === 'COMPLETED');
  const remainingNodes = nodes.filter(({ node }) => node.status !== 'COMPLETED');
  const isTerminal = isRollingRestartTerminalState(data.sm_state);
  const currentNodeIndex = isTerminal ? -1 : data.current_node_index;
  // Fallbacks cover backend states the UI doesn't know yet.
  const stateStyle = STATE_STYLE[data.sm_state] ?? 'info';
  const restartOnly = isRestartOnly(data);
  const texts = restartOnly ? RESTART_TEXTS : UPGRADE_TEXTS;
  const stateLabel =
    restartOnly && data.sm_state === 'UPGRADING_NODE'
      ? 'Restarting node'
      : (STATE_LABELS[data.sm_state] ?? 'In progress');
  const showProgress = !isTerminal && !isRollingRestartPaused(data.sm_state);

  return (
    <>
      <SectionHeading>
        {texts.heading}&nbsp;
        <Label bsStyle={stateStyle}>
          {stateLabel}
          {showProgress && <ProgressSpinner name="progress_activity" spin data-testid="rolling-upgrade-progress" />}
        </Label>
      </SectionHeading>
      {data.paused_reason && <Alert bsStyle="warning">{data.paused_reason}</Alert>}
      {data.last_error && <Alert bsStyle="danger">{data.last_error}</Alert>}
      {data.abort_requested && <Alert bsStyle="warning">{texts.abortRequested}</Alert>}
      <Row>
        <Col sm={6}>
          <NodesHeading>{texts.remainingHeading}</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={remainingNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage="No remaining nodes."
            showProgress={showProgress}
            restartOnly={restartOnly}
          />
        </Col>
        <Col sm={6}>
          <NodesHeading>{texts.completedHeading}</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={completedNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage={texts.noCompletedNodes}
            showProgress={showProgress}
            restartOnly={restartOnly}
          />
        </Col>
      </Row>
    </>
  );
};

export default OpenSearchRollingUpgradeNodes;
