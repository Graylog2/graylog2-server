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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DataNodesPageNavigation from 'components/datanode/DataNodePageNavigation';
import DataNodeList from 'components/datanode/DataNodeList/DataNodeList';

const DataNodesPage = () => (
  <DocumentTitle title="Data Nodes">
    <DataNodesPageNavigation />
    <PageHeader title="Data Nodes"
                documentationLink={{
                  title: 'Data Nodes documentation',
                  path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                }}>
      <span>
        Graylog data nodes offer a better integration with Graylog and simplify future updates. They allow you to index and search through all the messages in your Graylog message database.
      </span>
    </PageHeader>
    <Row className="content">
      <Col md={12}>
        <DataNodeList />
      </Col>
    </Row>
  </DocumentTitle>
);

export default DataNodesPage;
