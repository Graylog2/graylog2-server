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

import { LinkContainer } from 'components/common/router';
import { Col, Row, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import DashboardsOverview from 'views/components/dashboard/DashboardsOverview';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const DashboardsPage = () => {
  const sendTelemetry = useSendTelemetry();

  return (
    <DocumentTitle title="Dashboards">
      <PageHeader title="Dashboards"
                  actions={(
                    <IfPermitted permissions="dashboards:create">
                      <LinkContainer to={Routes.pluginRoute('DASHBOARDS_NEW')}>
                        <Button bsStyle="success"
                                onClick={() => {
                                  sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_CREATE_CLICKED, {
                                    app_pathname: 'dashboard',
                                    app_section: 'dashboard',
                                    app_action_value: 'dashboard-create-button',
                                  });
                                }}>Create new dashboard
                        </Button>
                      </LinkContainer>
                    </IfPermitted>
                  )}
                  documentationLink={{
                    title: 'Dashboard documentation',
                    path: DocsHelper.PAGES.DASHBOARDS,
                  }}>
        <span>
          Use dashboards to create specific views on your messages. Create a new dashboard here and add any graph or
          chart you create in other parts of Graylog with one click.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <DashboardsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default DashboardsPage;
