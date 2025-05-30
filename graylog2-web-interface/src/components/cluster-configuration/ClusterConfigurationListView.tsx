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
import styled from 'styled-components';

import { Table, Label } from 'components/bootstrap';
import { Spinner } from 'components/common';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import JournalState from 'components/nodes/JournalState';

import type { ClusterNodes } from './useClusterNodes';
import ClusterStatusLabel from './ClusterStatusLabel';
import ClusterActions from './ClusterActions';
import JvmHeapUsageText from './JvmHeapUsageText';

const SecondaryText = styled.div`
  span {
    font-size: small;
  }
`;

const NodeInfoTH = styled.th`
  width: 51%;
`;

type Props = {
  clusterNodes: ClusterNodes;
};

const getRoleLabels = (roles: string) =>
  roles.split(',').map((role) => (
    <span key={role}>
      <Label bsSize="xs">{role}</Label>&nbsp;
    </span>
  ));

const ClusterConfigurationListView = ({ clusterNodes }: Props) => (
  <Table>
    <thead>
      <tr>
        <NodeInfoTH>Node</NodeInfoTH>
        <th>Type</th>
        <th>Role</th>
        <th>State</th>
        <th className="text-right">Actions</th>
      </tr>
    </thead>
    <tbody>
      {clusterNodes.graylogNodes.map((graylogNode) => (
        <tr key={graylogNode.nodeName}>
          <td>
            <div>
              <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(graylogNode.nodeInfo.node_id)}>{graylogNode.nodeName}</Link>
            </div>
            <SecondaryText>
              <JournalState nodeId={graylogNode.nodeInfo.node_id} />
            </SecondaryText>
            <SecondaryText>
              <JvmHeapUsageText nodeId={graylogNode.nodeInfo.node_id} />
            </SecondaryText>
          </td>
          <td>{graylogNode.type}</td>
          <td>{getRoleLabels(graylogNode.role)}</td>
          <td>
            <ClusterStatusLabel node={graylogNode.nodeInfo} />
          </td>
          <td align="right">
            <ClusterActions node={graylogNode.nodeInfo} />
          </td>
        </tr>
      ))}
      {clusterNodes.dataNodes.map((dataNode) => (
        <tr key={dataNode.nodeName}>
          <td>
            <Link to={Routes.SYSTEM.CLUSTER.DATANODE_SHOW(dataNode.nodeInfo.node_id)}>{dataNode.nodeName}</Link>
          </td>
          <td>{dataNode.type}</td>
          <td>{getRoleLabels(dataNode.role)}</td>
          <td>
            <DataNodeStatusCell dataNode={dataNode.nodeInfo} />
          </td>
          <td align="right">
            <DataNodeActions dataNode={dataNode.nodeInfo} />
          </td>
        </tr>
      ))}
    </tbody>
    {clusterNodes.isLoading && <Spinner />}
  </Table>
);

export default ClusterConfigurationListView;
