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
import ClusterConfigurationListView from 'components/cluster-configuration/ClusterConfigurationListView';
import TableFetchContextProvider from 'components/common/PaginatedEntityTable/TableFetchContextProvider';
import type { SearchParams } from 'stores/PaginationTypes';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';

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
  const searchParams: SearchParams = { query: '', page: 1, pageSize: 0, sort: { attributeId: 'hostname', direction: 'asc' } };

  return (
    <DocumentTitle title="Cluster Configuration">
      <ClusterConfigurationPageNavigation />
      <div>
        <PageHeader title="Cluster Configuration">
          <span>
            This page provides a real-time overview of the nodes in your Graylog cluster.
            You can pause message processing at any time. The process buffers will not accept any new messages until
            you resume it. If the message journal is enabled for a node, which it is by default, incoming messages
            will be persisted to disk, even when processing is disabled.
          </span>
        </PageHeader>
        <HideOnCloud>
          <IndexerClusterHealth minimal />
        </HideOnCloud>
        <Row className="content">
          <Col xs={6}>
            <h2>Nodes</h2>
          </Col>
          {false && (
            <ViewTypeSwitchContainer xs={6}>
              <SegmentedControl data={VIEW_TYPES_SEGMENTS}
                                radius="sm"
                                value={viewType}
                                onChange={(newViewType) => setViewType(newViewType)} />
            </ViewTypeSwitchContainer>
          )}
          <Col md={12}>
            <TableFetchContextProvider refetch={clusterNodes.refetchDatanodes} searchParams={searchParams} attributes={[]}>
              {viewType === 'list' && (
                <ClusterConfigurationListView clusterNodes={clusterNodes} />
              )}
            </TableFetchContextProvider>
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default ClusterConfigurationPage;
