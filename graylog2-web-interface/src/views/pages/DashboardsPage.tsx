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
import * as React from 'react';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DashboardsOverview from 'views/components/dashboard/DashboardsOverview';
import CreateButton from 'components/common/CreateButton';

const DashboardsPage = () => (
  <DocumentTitle title="Dashboards">
    <PageHeader
      title="Dashboards"
      actions={<CreateButton entityKey="Dashboard" />}
      documentationLink={{
        title: 'Dashboard documentation',
        path: DocsHelper.PAGES.DASHBOARDS,
      }}>
      <span>
        Use dashboards to create specific views on your messages. Create a new dashboard here and add any graph or chart
        you create in other parts of the application with one click.
      </span>
    </PageHeader>

    <Row className="content">
      <Col md={12}>
        <DashboardsOverview />
      </Col>
    </Row>
  </DocumentTitle>
);

export default DashboardsPage;
