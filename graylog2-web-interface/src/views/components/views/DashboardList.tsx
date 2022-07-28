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
import React, { useEffect, useState, useCallback } from 'react';
import PropTypes from 'prop-types';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/bootstrap';
import { IfPermitted, PaginatedList, SearchForm, Spinner, EntityList, ShareButton } from 'components/common';
import EntityShareModal from 'components/permissions/EntityShareModal';
import QueryHelper from 'components/common/QueryHelper';
import type ViewClass from 'views/logic/views/View';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import DashboardListItem from './DashboardListItem';

import ViewTypeLabel from '../ViewTypeLabel';

const ItemActions = ({ dashboard, onDashboardDelete, setDashboardToShare }) => {
  return (
    <ButtonToolbar>
      <ShareButton entityId={dashboard.id} entityType="dashboard" onClick={() => setDashboardToShare(dashboard)} />
      <DropdownButton title="Actions" data-testid={`dashboard-actions-dropdown-${dashboard.id}`} id={`dashboard-actions-dropdown-${dashboard.id}`} pullRight>
        <IfPermitted permissions={[`view:edit:${dashboard.id}`, 'view:edit']} anyPermissions>
          <MenuItem onSelect={onDashboardDelete(dashboard)}>Delete</MenuItem>
        </IfPermitted>
      </DropdownButton>
    </ButtonToolbar>
  );
};

ItemActions.propTypes = {
  dashboard: PropTypes.object.isRequired,
  setDashboardToShare: PropTypes.func.isRequired,
  onDashboardDelete: PropTypes.func.isRequired,
};

const DashboardList = ({ pagination, handleSearch, handleDashboardDelete, dashboards }) => {
  const PAGE_SIZES = [10, 50, 100];
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter(PAGE_SIZES);
  const [query, setQuery] = useState('');
  const [dashboardToShare, setDashboardToShare] = useState<ViewClass>();

  const execSearch = useCallback(() => handleSearch(query, page, perPage), [handleSearch, page, perPage, query]);

  useEffect(() => {
    execSearch();
  }, [query, page, perPage, execSearch]);

  const onDashboardDelete = (dashboard) => () => {
    handleDashboardDelete(dashboard).then(() => {
      resetPage();
      execSearch();
    }, () => {});
  };

  const onSearch = (newQuery) => {
    resetPage();
    setQuery(newQuery);
  };

  const onReset = () => {
    resetPage();
    setQuery('');
  };

  if (!dashboards) {
    return <Spinner text="Loading dashboards..." />;
  }

  const items = dashboards.map((dashboard) => (
    <DashboardListItem key={`dashboard-${dashboard.id}`}
                       id={dashboard.id}
                       owner={dashboard.owner}
                       createdAt={dashboard.createdAt}
                       title={dashboard.title}
                       summary={dashboard.summary}
                       requires={dashboard.requires}
                       description={dashboard.description}>
      <ItemActions dashboard={dashboard} onDashboardDelete={onDashboardDelete} setDashboardToShare={setDashboardToShare} />
    </DashboardListItem>
  ));

  return (
    <>
      {dashboardToShare && (
        <EntityShareModal entityId={dashboardToShare.id}
                          entityType="dashboard"
                          description={`Search for a User or Team to add as collaborator on this ${ViewTypeLabel({ type: dashboardToShare.type })}.`}
                          entityTitle={dashboardToShare.title}
                          onClose={() => setDashboardToShare(undefined)} />
      )}
      <PaginatedList totalItems={pagination.total}
                     pageSizes={PAGE_SIZES}>
        <div style={{ marginBottom: 15 }}>
          <SearchForm onSearch={onSearch}
                      queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                      onReset={onReset}
                      topMargin={0} />
        </div>
        <EntityList items={items}
                    bsNoItemsStyle="success"
                    noItemsText="There are no dashboards present/matching the filter!" />
      </PaginatedList>
    </>
  );
};

DashboardList.propTypes = {
  dashboards: PropTypes.arrayOf(PropTypes.object),
  pagination: PropTypes.shape({
    total: PropTypes.number.isRequired,
    page: PropTypes.number.isRequired,
    perPage: PropTypes.number.isRequired,
  }).isRequired,
  handleSearch: PropTypes.func.isRequired,
  handleDashboardDelete: PropTypes.func.isRequired,
};

DashboardList.defaultProps = {
  dashboards: undefined,
};

export default DashboardList;
