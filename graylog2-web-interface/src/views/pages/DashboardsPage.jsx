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
// @flow strict
import React, { useEffect } from 'react';

// $FlowFixMe: imports from core need to be fixed in flow
import { LinkContainer } from 'components/graylog/router';
import { Col, Row, Button } from 'components/graylog';
import connect from 'stores/connect';
import { DocumentTitle, PageHeader, IfPermitted } from 'components/common/index';
import Routes from 'routing/Routes';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { DashboardsActions, DashboardsStore } from 'views/stores/DashboardsStore';
import type { DashboardsStoreState } from 'views/stores/DashboardsStore';
import ViewList from 'views/components/views/ViewList';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';

type Props = {
  dashboards: DashboardsStoreState,
};

const handleSearch = (query, page, perPage) => DashboardsActions.search(query, page, perPage);

const handleViewDelete = (view) => {
  // eslint-disable-next-line no-alert
  if (window.confirm(`Are you sure you want to delete "${view.title}"?`)) {
    return ViewManagementActions.delete(view);
  }

  return null;
};

const refreshDashboards = () => {
  DashboardsActions.search();
};

const DashboardsPage = ({ dashboards: { list, pagination } }: Props) => {
  useEffect(refreshDashboards, []);

  return (
    <DocumentTitle title="Dashboards">
      <span>
        <PageHeader title="Dashboards">
          <span>
            Use dashboards to create specific views on your messages. Create a new dashboard here and add any graph or
            chart you create in other parts of Graylog with one click.
          </span>

          <span>
            Take a look at the
            {' '}<DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="dashboard tutorial" />{' '}
            for lots of other useful tips.
          </span>

          <IfPermitted permissions="dashboards:create">
            <span>
              <LinkContainer to={Routes.pluginRoute('DASHBOARDS_NEW')}>
                <Button bsStyle="success" bsSize="lg">Create new dashboard</Button>
              </LinkContainer>
            </span>
          </IfPermitted>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <ViewList views={list}
                      pagination={pagination}
                      handleSearch={handleSearch}
                      handleViewDelete={handleViewDelete} />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

DashboardsPage.propTypes = {};

export default connect(DashboardsPage, { dashboards: DashboardsStore });
