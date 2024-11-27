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
import React, { useState } from 'react';

import { DocumentTitle, PageHeader } from 'components/common';
import { Col, Row, SegmentedControl, Table } from 'components/bootstrap';
import { NodesList } from 'components/nodes';
import { NodesStore } from 'stores/nodes/NodesStore';
import { useStore } from 'stores/connect';
import DataNodeList from 'components/datanode/DataNodeList/DataNodeList';
import useClusterNodes from 'components/cluster-configuration/useClusterNodes';
import MoreActions from 'components/common/EntityDataTable/MoreActions';

const VIEW_TYPES_SEGMENTS = [
  {
    value: 'list' as const,
    label: 'Table view',
  },
  {
    value: 'icons' as const,
    label: 'Cluster view',
  },
];

type ViewTypesSegments = 'list' | 'icons';

const ClusterConfigurationPage = () => {
  const { nodes } = useStore(NodesStore);
  const [viewType, setViewType] = useState<ViewTypesSegments>('list');
  const { graylogNodes, dataNodes } = useClusterNodes();

  return (
    <DocumentTitle title="Cluster Configuration">
      <div>
        <PageHeader title="Cluster Configuration">
          <span>
            This page provides a real-time overview of the nodes in your Graylog cluster.
            You can pause message processing at any time. The process buffers will not accept any new messages until
            you resume it. If the message journal is enabled for a node, which it is by default, incoming messages
            will be persisted to disk, even when processing is disabled.
          </span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            <h2>Nodes</h2>
            <br/>
            <SegmentedControl data={VIEW_TYPES_SEGMENTS}
                              radius="sm"
                              value={viewType}
                              onChange={(newViewType) => setViewType(newViewType)} />
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
                {graylogNodes.map((graylogNode) => (
                  <tr>
                    <td>{graylogNode.nodeName}</td>
                    <td>{graylogNode.type}</td>
                    <td>{graylogNode.role}</td>
                    <td>{graylogNode.state}</td>
                    <td><MoreActions /></td>
                  </tr>
                ))}
                {dataNodes.map((dataNode) => (
                  <tr>
                    <td>{dataNode.nodeName}</td>
                    <td>{dataNode.type}</td>
                    <td>{dataNode.role}</td>
                    <td>{dataNode.state}</td>
                    <td><MoreActions /></td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Col>
        </Row>
        <NodesList nodes={nodes} />
        <DataNodeList />
      </div>
    </DocumentTitle>
  );
};

export default ClusterConfigurationPage;
