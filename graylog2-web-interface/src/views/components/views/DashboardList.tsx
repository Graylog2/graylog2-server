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
import React, { useState, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/bootstrap';
import { IfPermitted, PaginatedList, SearchForm, Spinner, ShareButton, HoverForHelp } from 'components/common';
import EntityShareModal from 'components/permissions/EntityShareModal';
import QueryHelper from 'components/common/QueryHelper';
import type ViewClass from 'views/logic/views/View';
import type { ColumnRenderers, Sort } from 'components/common/EntityDataTable';
import EntityDataTable from 'components/common/EntityDataTable';
import type { Stream } from 'stores/streams/StreamsStore';
import { Link } from 'components/common/router';
import type View from 'views/logic/views/View';
import Routes from 'routing/Routes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { DashboardsActions } from 'views/stores/DashboardsStore';
import useDashboards from 'views/logic/dashboards/useDashboards';
import usePluginEntities from 'hooks/usePluginEntities';

import ViewTypeLabel from '../ViewTypeLabel';

type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
  sort: Sort
}

type Plugin = {
  name: string,
  url: string,
};

// eslint-disable-next-line no-alert
const defaultDashboardDeletionHook = async (view: View) => window.confirm(`Are you sure you want to delete "${view.title}"?`);

const INITIAL_COLUMNS = ['title', 'description', 'summary'];

const COLUMN_DEFINITIONS = [
  { id: 'created_at', title: 'Created At', sortable: true },
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: true },
  { id: 'summary', title: 'Summary', sortable: true },
  { id: 'owner', title: 'Owner', sortable: true },
];

const missingRequirements = (requires, requirementsProvided) => (
  Object.entries(requires)
    .filter(([require]) => !requirementsProvided.includes(require))
    .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {})
);

const Requirements = ({ requirements }: { requirements: Array<Plugin> }) => (
  <div>
    {Object.values(requirements).map(({ url, name }) => (
      <a href={url} target="_blank" rel="noopener noreferrer"><strong>{name}</strong></a>
    ))}
  </div>
);

const customColumnRenderers = (requirementsProvided): ColumnRenderers<View> => ({
  title: {
    renderCell: ({ id, requires, title }) => {
      const _missingRequirements = missingRequirements(requires, requirementsProvided);
      const isMissingRequirements = Object.keys(_missingRequirements).length > 0;

      if (isMissingRequirements) {
        return <>{title} <HoverForHelp title="Missing Requirements"><Requirements requirements={_missingRequirements} /></HoverForHelp></>;
      }

      return <Link to={Routes.pluginRoute('DASHBOARDS_VIEWID')(id)}>{title}</Link>;
    },
  },
});

const ItemActions = ({ dashboard, onDashboardDelete, setDashboardToShare }) => {
  return (
    <ButtonToolbar>
      <ShareButton bsSize="xsmall" entityId={dashboard.id} entityType="dashboard" onClick={() => setDashboardToShare(dashboard)} />
      <DropdownButton bsSize="xsmall" title="More Actions" data-testid={`dashboard-actions-dropdown-${dashboard.id}`} id={`dashboard-actions-dropdown-${dashboard.id}`} pullRight>
        <IfPermitted permissions={[`view:edit:${dashboard.id}`, 'view:edit']} anyPermissions>
          <MenuItem onSelect={() => onDashboardDelete(dashboard)}>Delete</MenuItem>
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

const DashboardList = () => {
  const paginationQueryParameter = usePaginationQueryParameter(undefined, 20);
  // const [searchQuery, setSearchQuery] = useState('');
  const [visibleColumns, setVisibleColumns] = useState(INITIAL_COLUMNS);
  const [dashboardToShare, setDashboardToShare] = useState<ViewClass>();
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: paginationQueryParameter.page,
    pageSize: paginationQueryParameter.pageSize,
    query: '',
    sort: {
      columnId: 'title',
      order: 'asc',
    },
  });
  const { list: dashboards, pagination } = useDashboards(searchParams.query, searchParams.page, searchParams.pageSize, searchParams.sort.columnId, searchParams.sort.order);

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setSearchParams((cur) => ({ ...cur, query: newQuery }));
  }, [paginationQueryParameter]);

  const requirementsProvided = usePluginEntities('views.requires.provided');
  const columnRenderers = useMemo(() => customColumnRenderers(requirementsProvided), []);

  const handleDashboardDelete = useCallback(async (view: View) => {
    const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

    const result = await iterateConfirmationHooks([...pluginDashboardDeletionHooks, defaultDashboardDeletionHook], view);

    if (result) {
      await ViewManagementActions.delete(view);
      await DashboardsActions.search(searchParams.query, searchParams.page, searchParams.pageSize, searchParams.sort.columnId, searchParams.sort.order);
      paginationQueryParameter.resetPage();
    }
  }, [paginationQueryParameter, searchParams]);

  const onColumnsChange = useCallback((newVisibleColumns: Array<string>) => {
    setVisibleColumns(newVisibleColumns);
  }, []);

  const renderStreamActions = useCallback((dashboard: Stream) => (
    <ItemActions dashboard={dashboard}
                 onDashboardDelete={handleDashboardDelete}
                 setDashboardToShare={setDashboardToShare} />
  ), [handleDashboardDelete]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onPageChange = useCallback(
    (newPage: number, newPageSize: number) => setSearchParams((cur) => ({ ...cur, page: newPage, pageSize: newPageSize })),
    [],
  );

  const onSortChange = useCallback((newSort: Sort) => {
    setSearchParams((cur) => ({ ...cur, sort: newSort, page: 1 }));
    paginationQueryParameter.resetPage();
  }, [paginationQueryParameter]);

  if (!dashboards) {
    return <Spinner text="Loading dashboards..." />;
  }
  //
  // const items = dashboards.map((dashboard) => (
  //   <DashboardListItem key={`dashboard-${dashboard.id}`}
  //                      id={dashboard.id}
  //                      owner={dashboard.owner}
  //                      createdAt={dashboard.createdAt}
  //                      title={dashboard.title}
  //                      summary={dashboard.summary}
  //                      requires={dashboard.requires}
  //                      description={dashboard.description}>
  //     <ItemActions dashboard={dashboard} onDashboardDelete={onDashboardDelete} setDashboardToShare={setDashboardToShare} />
  //   </DashboardListItem>
  // ));

  return (
    <>
      {dashboardToShare && (
        <EntityShareModal entityId={dashboardToShare.id}
                          entityType="dashboard"
                          description={`Search for a User or Team to add as collaborator on this ${ViewTypeLabel({ type: dashboardToShare.type })}.`}
                          entityTitle={dashboardToShare.title}
                          onClose={() => setDashboardToShare(undefined)} />
      )}
      <PaginatedList onChange={onPageChange}
                     pageSize={searchParams.pageSize}
                     totalItems={pagination.total}>
        <div style={{ marginBottom: 15 }}>
          <SearchForm onSearch={onSearch}
                      queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                      onReset={onReset}
                      topMargin={0} />
        </div>
        <EntityDataTable data={dashboards}
                         visibleColumns={visibleColumns}
                         onColumnsChange={onColumnsChange}
                         onSortChange={onSortChange}
                         activeSort={searchParams.sort}
                         rowActions={renderStreamActions}
                         columnRenderers={columnRenderers}
                         columnDefinitions={COLUMN_DEFINITIONS} />
      </PaginatedList>
    </>
  );
};

export default DashboardList;
