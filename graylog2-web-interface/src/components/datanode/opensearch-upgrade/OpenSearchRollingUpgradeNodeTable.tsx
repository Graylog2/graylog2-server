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

import { Label, Table } from 'components/bootstrap';
import { Icon } from 'components/common';

import type { OpenSearchVersionNode } from './hooks/useOpenSearchClusterStats';
import type { RollingRestartNode, RollingRestartNodeStatus } from './rollingRestartTypes';

export type RollingUpgradeNodeWithContext = {
  node: RollingRestartNode;
  index: number;
  versionNode?: OpenSearchVersionNode;
};

const CellDetail = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
    margin-top: ${theme.spacings.xxs};
  `,
);

const NODE_STATUS_STYLE: Record<RollingRestartNodeStatus, 'default' | 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING: 'default',
  RESTARTING: 'info',
  STARTED: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
  SKIPPED: 'default',
};

const NODE_STATUS_LABELS: Record<RollingRestartNodeStatus, string> = {
  PENDING: 'Waiting',
  RESTARTING: 'Restarting',
  STARTED: 'Started',
  COMPLETED: 'Upgraded',
  FAILED: 'Failed',
  SKIPPED: 'Skipped',
};

const IN_FLIGHT_NODE_STATUSES: Array<RollingRestartNodeStatus> = ['RESTARTING', 'STARTED'];

const nodeName = (node: RollingRestartNode, versionNode: OpenSearchVersionNode | undefined) =>
  versionNode?.datanode?.node_name ?? versionNode?.datanode?.hostname ?? node.hostname;

const opensearchVersion = (node: RollingRestartNode, versionNode: OpenSearchVersionNode | undefined) => {
  if (!versionNode) {
    return 'Unknown';
  }

  return node.status === 'COMPLETED'
    ? (versionNode.available_version ?? versionNode.current_version)
    : versionNode.current_version;
};

const NodeIdentityCell = ({
  isCurrent,
  node,
  versionNode = undefined,
}: {
  isCurrent: boolean;
  node: RollingRestartNode;
  versionNode?: OpenSearchVersionNode;
}) => {
  const ip = versionNode?.datanode?.ip;
  const isManagerNode = versionNode?.datanode?.manager_node;

  return (
    <td>
      <div>
        {nodeName(node, versionNode)}&nbsp;
        {isManagerNode && <Label bsStyle="default">manager</Label>}
        {isCurrent && (
          <>
            &nbsp;
            <Label bsStyle="primary">current</Label>
          </>
        )}
      </div>
      {ip && <CellDetail>{ip}</CellDetail>}
      {node.last_error && (
        <div>
          <Label bsStyle="danger">{node.last_error}</Label>
        </div>
      )}
    </td>
  );
};

const OpenSearchVersionCell = ({
  node,
  versionNode = undefined,
}: {
  node: RollingRestartNode;
  versionNode?: OpenSearchVersionNode;
}) => <td>{opensearchVersion(node, versionNode)}</td>;

const NodeStatusCell = ({ node, showProgress }: { node: RollingRestartNode; showProgress: boolean }) => (
  <td align="right">
    <Label bsStyle={NODE_STATUS_STYLE[node.status]}>
      {NODE_STATUS_LABELS[node.status]}
      &nbsp;
      {node.status === 'COMPLETED' && <Icon name="check" />}
      {showProgress && IN_FLIGHT_NODE_STATUSES.includes(node.status) && (
        <Icon name="progress_activity" spin data-testid="node-upgrade-progress" />
      )}
    </Label>
  </td>
);

const OpenSearchRollingUpgradeNodeTable = ({
  emptyMessage,
  nodes,
  currentNodeIndex,
  showProgress,
}: {
  emptyMessage: string;
  nodes: Array<RollingUpgradeNodeWithContext>;
  currentNodeIndex: number;
  showProgress: boolean;
}) => (
  <Table>
    <thead>
      <tr>
        <th>Data Node</th>
        <th>OpenSearch version</th>
        <th aria-label="Status" />
      </tr>
    </thead>
    <tbody>
      {nodes.map(({ node, index, versionNode }) => (
        <tr key={node.datanode_id}>
          <NodeIdentityCell node={node} versionNode={versionNode} isCurrent={index === currentNodeIndex} />
          <OpenSearchVersionCell node={node} versionNode={versionNode} />
          <NodeStatusCell node={node} showProgress={showProgress} />
        </tr>
      ))}
      {!nodes.length && (
        <tr>
          <td colSpan={3}>{emptyMessage}</td>
        </tr>
      )}
    </tbody>
  </Table>
);

export default OpenSearchRollingUpgradeNodeTable;
