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

import type { OpenSearchVersionNode } from './hooks/useOpenSearchClusterStats';
import OpenSearchRollingUpgradeNodeTable from './OpenSearchRollingUpgradeNodeTable';
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';
import { isRollingRestartTerminalState } from './rollingRestartTypes';

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

const STATE_LABELS: Record<RollingRestartState, string> = {
  PREPARING_CLUSTER: 'Preparing cluster',
  SELECTING_NEXT_NODE: 'Selecting next node',
  STOPPING_NODE: 'Stopping node',
  WAITING_NODE_LEFT: 'Waiting for node to leave',
  STARTING_NODE: 'Starting node',
  WAITING_NODE_JOINED: 'Waiting for node to rejoin',
  REENABLING_ALLOCATION: 'Re-enabling allocation',
  WAITING_GREEN: 'Waiting for green cluster',
  PAUSED_WAITING_GREEN: 'Paused waiting for green cluster',
  FINALIZING: 'Finalizing',
  COMPLETED: 'Completed',
  ABORTED: 'Aborted',
  FAILED: 'Failed',
};

const STATE_STYLE: Record<RollingRestartState, 'default' | 'info' | 'success' | 'warning' | 'danger'> = {
  PREPARING_CLUSTER: 'info',
  SELECTING_NEXT_NODE: 'info',
  STOPPING_NODE: 'warning',
  WAITING_NODE_LEFT: 'warning',
  STARTING_NODE: 'info',
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
  const currentNodeIndex = isRollingRestartTerminalState(data.sm_state) ? -1 : data.current_node_index;

  return (
    <>
      <SectionHeading>
        OpenSearch rolling upgrade&nbsp;
        <Label bsStyle={STATE_STYLE[data.sm_state]}>
          {STATE_LABELS[data.sm_state]}
        </Label>
      </SectionHeading>
      {data.paused_reason && <Alert bsStyle="warning">{data.paused_reason}</Alert>}
      {data.last_error && <Alert bsStyle="danger">{data.last_error}</Alert>}
      {data.abort_requested && (
        <Alert bsStyle="warning">Abort requested. The upgrade will stop after the current step.</Alert>
      )}
      <Row>
        <Col sm={6}>
          <NodesHeading>Waiting for OpenSearch upgrade</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={remainingNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage="No remaining nodes."
          />
        </Col>
        <Col sm={6}>
          <NodesHeading>Upgraded to target OpenSearch</NodesHeading>
          <OpenSearchRollingUpgradeNodeTable
            nodes={completedNodes}
            currentNodeIndex={currentNodeIndex}
            emptyMessage="No upgraded nodes yet."
          />
        </Col>
      </Row>
    </>
  );
};

export default OpenSearchRollingUpgradeNodes;
