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
import styled from 'styled-components';

import { DocumentTitle, Icon, PageHeader } from 'components/common';
import { Col, Row, SegmentedControl } from 'components/bootstrap';
import useClusterNodes from 'components/cluster-configuration/useClusterNodes';
import ClusterConfigurationClusterView from 'components/cluster-configuration/ClusterConfigurationClusterView';
import ClusterConfigurationTableView from 'components/cluster-configuration/ClusterConfigurationTableView';

const ViewTypeSwitchContainer = styled(Col)`
  display: flex;
  justify-content: right;
`;

const VIEW_TYPES_SEGMENTS = [
  {
    value: 'list' as const,
    label: (<Icon name="list" />),
  },
  {
    value: 'cards' as const,
    label: (<Icon name="account_tree" type='regular' />),
  },
];

type ViewTypesSegments = 'list' | 'cards';

const ClusterConfigurationPage = () => {
  const [viewType, setViewType] = useState<ViewTypesSegments>('list');
  const clusterNodes = useClusterNodes();

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
          <Col xs={6}>
            <h2>Nodes</h2>
          </Col>
          <ViewTypeSwitchContainer xs={6}>
            <SegmentedControl data={VIEW_TYPES_SEGMENTS}
                              radius="sm"
                              value={viewType}
                              onChange={(newViewType) => setViewType(newViewType)} />
          </ViewTypeSwitchContainer>
          <Col md={12}>
            {viewType === 'list' && (
              <ClusterConfigurationTableView clusterNodes={clusterNodes} />
            )}
            {viewType === 'cards' && (
              <ClusterConfigurationClusterView />
            )}
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default ClusterConfigurationPage;
