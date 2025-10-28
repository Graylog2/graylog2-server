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

import { DocumentTitle, PageHeader } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';
import GraylogNodesExpandable from 'components/cluster-configuration/GraylogNodesExpandable';
import DataNodesExpandable from 'components/cluster-configuration/DataNodesExpandable';

const SectionCol = styled(Col)`
  margin-bottom: 15px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const ClusterConfigurationPage = () => (
  <DocumentTitle title="Cluster Configuration">
    <ClusterConfigurationPageNavigation />
    <div>
      <PageHeader title="Cluster Configuration">
        <span>
          This page provides a real-time overview of the nodes in your cluster. You can pause message processing at
          any time. The process buffers will not accept any new messages until you resume it. If the message journal
          is enabled for a node, which it is by default, incoming messages will be persisted to disk, even when
          processing is disabled.
        </span>
      </PageHeader>
      <HideOnCloud>
        <IndexerClusterHealth minimal />
      </HideOnCloud>
      <Row className="content">
        <SectionCol md={12}>
          <GraylogNodesExpandable />
        </SectionCol>
        <SectionCol md={12}>
          <DataNodesExpandable />
        </SectionCol>
      </Row>
    </div>
  </DocumentTitle>
);

export default ClusterConfigurationPage;
