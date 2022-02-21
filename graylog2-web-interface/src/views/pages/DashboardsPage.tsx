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
import { useCallback, useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { LinkContainer } from 'components/common/router';
import { Col, Row, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import DashboardList from 'views/components/views/DashboardList';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import useDashboards from 'views/logic/dashboards/useDashboards';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';

import type View from '../logic/views/View';

// eslint-disable-next-line no-alert
const defaultDashboardDeletionHook = async (view: View) => window.confirm(`Are you sure you want to delete "${view.title}"?`);

const handleDashboardDelete = async (view: View) => {
  const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

  const result = await iterateConfirmationHooks([...pluginDashboardDeletionHooks, defaultDashboardDeletionHook], view);

  return result === true ? ViewManagementActions.delete(view) : Promise.reject();
};

type SearchQuery = {
  query: string,
  page: number,
  perPage: number,
};

const DashboardsPage = () => {
  const [searchQuery, setSearchQuery] = useState<SearchQuery>({ query: '', page: 1, perPage: 10 });
  const handleSearch = useCallback((query: string, page: number, perPage: number) => setSearchQuery({ query, page, perPage }), []);
  const { list, pagination } = useDashboards(searchQuery);

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
            <DashboardList dashboards={list}
                           pagination={pagination}
                           handleSearch={handleSearch}
                           handleDashboardDelete={handleDashboardDelete} />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

DashboardsPage.propTypes = {};

export default DashboardsPage;
