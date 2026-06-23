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

import { Alert, Col, Label, Row, Table } from 'components/bootstrap';
import { Icon, Timestamp } from 'components/common';

import type {
  RollingRestartJob,
  RollingRestartNode,
  RollingRestartNodeStatus,
  RollingRestartState,
} from './rollingRestartTypes';

type NodeWithIndex = {
  node: RollingRestartNode;
  index: number;
};

type Props = {
  job: RollingRestartJob | null | undefined;
};

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

const NODE_STATUS_STYLE: Record<RollingRestartNodeStatus, 'default' | 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING: 'default',
  STOPPING: 'warning',
  STOPPED: 'warning',
  STARTING: 'info',
  STARTED: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
  SKIPPED: 'default',
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

const NodeIdentityCell = ({ node, isCurrent }: { node: RollingRestartNode; isCurrent: boolean }) => (
  <td>
    <div>
      {node.hostname}&nbsp;
      {isCurrent && (
        <Label bsStyle="info">
          current
        </Label>
      )}
    </div>
    <div>
      <i>{node.datanode_id}</i>
    </div>
    {node.last_error && (
      <div>
        <Label bsStyle="danger">
          {node.last_error}
        </Label>
      </div>
    )}
  </td>
);

const NodeStatusCell = ({ node }: { node: RollingRestartNode }) => (
  <td align="right">
    <Label bsStyle={NODE_STATUS_STYLE[node.status]}>
      {node.status}
      &nbsp;
      {node.status === 'COMPLETED' && <Icon name="check" />}
    </Label>
  </td>
);

const TimeCell = ({ node }: { node: RollingRestartNode }) => {
  const timestamp = node.finished_at ?? node.started_at;

  return <td>{timestamp ? <Timestamp dateTime={timestamp} /> : '-'}</td>;
};

const NodesTable = ({
  emptyMessage,
  nodes,
  currentNodeIndex,
}: {
  emptyMessage: string;
  nodes: Array<NodeWithIndex>;
  currentNodeIndex: number;
}) => (
  <Table>
    <tbody>
      {nodes.map(({ node, index }) => (
        <tr key={node.datanode_id}>
          <NodeIdentityCell node={node} isCurrent={index === currentNodeIndex} />
          <TimeCell node={node} />
          <NodeStatusCell node={node} />
        </tr>
      ))}
      {!nodes.length && (
        <tr>
          <td>{emptyMessage}</td>
        </tr>
      )}
    </tbody>
  </Table>
);

const OpenSearchRollingUpgradeNodes = ({ job }: Props) => {
  const data = job?.data;

  if (!data) {
    return <Alert bsStyle="info">No OpenSearch rolling restart has been started yet.</Alert>;
  }

  const nodes = data.nodes.map((node, index) => ({ node, index }));
  const completedNodes = nodes.filter(({ node }) => node.status === 'COMPLETED');
  const remainingNodes = nodes.filter(({ node }) => node.status !== 'COMPLETED');

  return (
    <>
      <br />
      <h3>
        OpenSearch rolling restart&nbsp;
        <Label bsStyle={STATE_STYLE[data.sm_state]}>
          {STATE_LABELS[data.sm_state]}
        </Label>
      </h3>
      {data.paused_reason && <Alert bsStyle="warning">{data.paused_reason}</Alert>}
      {data.last_error && <Alert bsStyle="danger">{data.last_error}</Alert>}
      {data.abort_requested && (
        <Alert bsStyle="warning">Abort requested. The restart will stop after the current step.</Alert>
      )}
      <Row>
        <Col sm={6}>
          <h3>Remaining OpenSearch Restart</h3>
          <br />
          <NodesTable
            nodes={remainingNodes}
            currentNodeIndex={data.current_node_index}
            emptyMessage="No remaining nodes."
          />
        </Col>
        <Col sm={6}>
          <h3>Completed OpenSearch Restart</h3>
          <br />
          <NodesTable
            nodes={completedNodes}
            currentNodeIndex={data.current_node_index}
            emptyMessage="No completed nodes yet."
          />
        </Col>
      </Row>
    </>
  );
};

export default OpenSearchRollingUpgradeNodes;
