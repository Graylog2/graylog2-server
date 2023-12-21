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

const DataNodesMigrationPage = () => (
  <DocumentTitle title="Data Nodes Migration">
    <DataNodesPageNavigation />
    <PageHeader title="Data Nodes Migration"
                documentationLink={{
                  title: 'Data Nodes documentation',
                  path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                }}>
      <span>
        Graylog Data Nodes offer a better integration with Graylog and simplify future updates. They allow you to index and search through all the messages in your Graylog message database.
      </span>
    </PageHeader>
    <Row className="content">
      <Col md={12}>
        TODO: Migration Component
      </Col>
    </Row>
  </DocumentTitle>
);

export default DataNodesMigrationPage;
