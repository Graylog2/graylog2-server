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
import { Section, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import Routes from 'routing/Routes';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';

import type { ClusterNodes } from './useClusterNodes';

const NodeInfoTH = styled.th`
  width: 51%;
`;

const StyledTable = styled(Table)`
  table-layout: fixed;
  width: 100%;

  th,
  td {
    white-space: normal !important;
    overflow-wrap: anywhere;
    word-break: break-word;
  }
`;

const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

type Props = {
  clusterNodes: ClusterNodes;
};

const getRoleLabels = (roles: string) =>
  roles.split(',').map((role) => (
    <span key={role}>
      <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
    </span>
  ));

const DataNodesExpandable = ({ clusterNodes }: Props) => (
  <Section
    title="Data Nodes"
    collapsible
  >
    <StyledTable>
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
    </StyledTable>
  </Section>
);

export default DataNodesExpandable;
