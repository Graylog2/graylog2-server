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
import { useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { LinkContainer } from 'components/common/router';
import { Col, Row, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, IfPermitted, NoEntitiesExist } from 'components/common';
import Routes from 'routing/Routes';
import DashboardList from 'views/components/views/DashboardList';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import useDashboards from 'views/logic/dashboards/useDashboards';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import DocsHelper from 'util/DocsHelper';

import type View from '../logic/views/View';
import { DashboardsActions } from '../stores/DashboardsStore';

// eslint-disable-next-line no-alert
const defaultDashboardDeletionHook = async (view: View) => window.confirm(`Are you sure you want to delete "${view.title}"?`);

const DashboardsPage = () => {
  const { page, pageSize, resetPage } = usePaginationQueryParameter();
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (newQuery = searchQuery) => {
    resetPage();
    setSearchQuery(newQuery);
  };

  const handleDashboardDelete = async (view: View) => {
    const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

    const result = await iterateConfirmationHooks([...pluginDashboardDeletionHooks, defaultDashboardDeletionHook], view);

    if (result) {
      await ViewManagementActions.delete(view);
      await DashboardsActions.search(searchQuery, page, pageSize);
      resetPage();
    }
  };

  const { list, pagination } = useDashboards(searchQuery, page, pageSize);

  const createDashboardButtonText = (
    <IfPermitted permissions="dashboards:create">
      <LinkContainer to={Routes.pluginRoute('DASHBOARDS_NEW')}>
        <Button bsSize="small" bsStyle="success" className="btn-text">Create one now</Button>
      </LinkContainer>
    </IfPermitted>
  );

  const noDashboardsFound = (
    <NoEntitiesExist>
      No dashboards have been created yet. {createDashboardButtonText}
    </NoEntitiesExist>
  );

  return (
    <DocumentTitle title="Dashboards">
      <PageHeader title="Dashboards"
                  actions={(
                    <IfPermitted permissions="dashboards:create">
                      <LinkContainer to={Routes.pluginRoute('DASHBOARDS_NEW')}>
                        <Button bsStyle="success">Create new dashboard</Button>
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
          {!list?.length && !searchQuery
            ? noDashboardsFound
            : (
              <DashboardList dashboards={list}
                             pagination={pagination}
                             handleSearch={handleSearch}
                             handleDashboardDelete={handleDashboardDelete} />
            )}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default DashboardsPage;
