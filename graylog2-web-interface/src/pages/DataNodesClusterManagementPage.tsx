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

import ProductName from 'brand-customization/ProductName';
import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import ClusterManagementOverview from 'components/datanode/ClusterManagement/ClusterManagementOverview';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import { widgetDragHandleClass, widgetActionsMenuClass } from 'views/components/widgets/Constants';

const StyledCol = styled(Col)`
  footer,
  .license-notification-container,
  .query-tab-create,
  .query-config-btn,
  .fa-star,
  .react-resizable-handle,
  button:has(.fa-copy),
  button:has(.fa-chevron-down),
  .${widgetDragHandleClass}, .${widgetActionsMenuClass} {
    display: none;
  }

  .react-grid-layout,
  .container-fluid > .row:first-of-type {
    pointer-events: none;
  }
`;

const DataNodesClusterManagementPage = () => (
  <DocumentTitle title="Data Node Dashboard">
    <ClusterConfigurationPageNavigation />
    <PageHeader
      title="Data Node Dashboard"
      documentationLink={{
        title: 'Data Nodes documentation',
        path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
      }}>
      <span>
        <ProductName /> Data Nodes offer a better integration with <ProductName /> and simplify future updates. They
        allow you to index and search through all the messages in your <ProductName /> message database.
      </span>
    </PageHeader>
    <Row className="content">
      <StyledCol md={12}>
        <ClusterManagementOverview />
      </StyledCol>
    </Row>
  </DocumentTitle>
);

export default DataNodesClusterManagementPage;
