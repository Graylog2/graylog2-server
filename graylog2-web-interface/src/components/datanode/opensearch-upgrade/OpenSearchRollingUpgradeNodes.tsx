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
import { useTranslation } from 'react-i18next';

import { Alert, Col, Label, Row } from 'components/bootstrap';
import { Icon } from 'components/common';

import type { OpenSearchVersionNode } from './hooks/useOpenSearchClusterStats';
import OpenSearchRollingUpgradeNodeTable from './OpenSearchRollingUpgradeNodeTable';
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';
import { isRollingRestartPaused, isRollingRestartTerminalState } from './rollingRestartTypes';

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
  PREPARING_CLUSTER: 'rolling_upgrade_nodes.state.preparing_cluster',
  SELECTING_NEXT_NODE: 'rolling_upgrade_nodes.state.selecting_next_node',
  UPGRADING_NODE: 'rolling_upgrade_nodes.state.upgrading_node',
  WAITING_NODE_JOINED: 'rolling_upgrade_nodes.state.waiting_node_joined',
  REENABLING_ALLOCATION: 'rolling_upgrade_nodes.state.reenabling_allocation',
  WAITING_GREEN: 'rolling_upgrade_nodes.state.waiting_green',
  PAUSED_WAITING_GREEN: 'rolling_upgrade_nodes.state.paused_waiting_green',
  FINALIZING: 'rolling_upgrade_nodes.state.finalizing',
  COMPLETED: 'rolling_upgrade_nodes.state.completed',
  ABORTED: 'rolling_upgrade_nodes.state.aborted',
  FAILED: 'rolling_upgrade_nodes.state.failed',
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
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const data = job?.data;

  if (!data) {
    return <Alert bsStyle="info">{t('rolling_upgrade_nodes.not_started')}</Alert>;
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
  const stateLabel = t(STATE_LABELS[data.sm_state] ?? 'rolling_upgrade_nodes.state.in_progress');
  const showProgress = !isTerminal && !isRollingRestartPaused(data.sm_state);

  return (
    <>
      <SectionHeading>
        {t('rolling_upgrade_nodes.title')}&nbsp;
        <Label bsStyle={stateStyle}>
          {stateLabel}
          {showProgress && <ProgressSpinner name="progress_activity" spin data-testid="rolling-upgrade-progress" />}
        </Label>
      </SectionHeading>
      {data.paused_reason && <Alert bsStyle="warning">{data.paused_reason}</Alert>}
      {data.last_error && <Alert bsStyle="danger">{data.last_error}</Alert>}
      {data.abort_requested && <Alert bsStyle="warning">{t('rolling_upgrade_nodes.abort_requested')}</Alert>}
      <Row>
        <Col sm={6}>
          <NodesHeading>{t('rolling_upgrade_nodes.waiting_heading')}</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={remainingNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage={t('rolling_upgrade_nodes.no_remaining_nodes')}
            showProgress={showProgress}
          />
        </Col>
        <Col sm={6}>
          <NodesHeading>{t('rolling_upgrade_nodes.upgraded_heading')}</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={completedNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage={t('rolling_upgrade_nodes.no_upgraded_nodes')}
            showProgress={showProgress}
          />
        </Col>
      </Row>
    </>
  );
};

export default OpenSearchRollingUpgradeNodes;
