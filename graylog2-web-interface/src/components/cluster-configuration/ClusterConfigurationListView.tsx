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

import { Table, Label } from 'components/bootstrap';
import MoreActions from 'components/common/EntityDataTable/MoreActions';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import type { ClusterNodes } from './useClusterNodes';
import ClusterStatusLabel from './ClusterStatusLabel';

type Props = {
  clusterNodes: ClusterNodes,
}

const getRoleLabels = (roles: string) => roles.split(',').map((role) => (
  <span key={role}>
    <Label bsSize="xs">{role}</Label>&nbsp;
  </span>
));

const ClusterConfigurationListView = ({ clusterNodes }: Props) => (
  <Table>
    <thead>
      <tr>
        <th>Node</th>
        <th>Type</th>
        <th>Role</th>
        <th>State</th>
        <th className="text-right">Actions</th>
      </tr>
    </thead>
    <tbody>
      {clusterNodes.graylogNodes.map((graylogNode) => (
        <tr key={graylogNode.nodeName}>
          <td><Link to={Routes.SYSTEM.NODES.SHOW(graylogNode.nodeInfo.node_id)}>{graylogNode.nodeName}</Link></td>
          <td>{graylogNode.type}</td>
          <td>{getRoleLabels(graylogNode.role)}</td>
          <td><ClusterStatusLabel node={graylogNode.nodeInfo} /></td>
          <td align='right'><MoreActions /></td>
        </tr>
      ))}
      {clusterNodes.dataNodes.map((dataNode) => (
        <tr key={dataNode.nodeName}>
          <td><Link to={Routes.SYSTEM.DATANODES.SHOW(dataNode.nodeInfo.node_id)}>{dataNode.nodeName}</Link></td>
          <td>{dataNode.type}</td>
          <td>{getRoleLabels(dataNode.role)}</td>
          <td><DataNodeStatusCell dataNode={dataNode.nodeInfo} /></td>
          <td align='right'><MoreActions /></td>
        </tr>
      ))}
    </tbody>
  </Table>
);

export default ClusterConfigurationListView;
