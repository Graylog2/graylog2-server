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

import { PaginatedList, SearchForm, Spinner } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';
import type { ColumnRenderers, Sort } from 'components/common/EntityDataTable';
import EntityDataTable from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useDashboards from 'views/logic/dashboards/useDashboards';
import usePluginEntities from 'hooks/usePluginEntities';
import DashboardActions from 'views/components/dashboard/DashboardsOverview/DashboardActions';
import { Alert } from 'components/bootstrap';

import TitleCell from './TitleCell';

type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
  sort: Sort
}

const INITIAL_COLUMNS = ['title', 'description', 'summary'];

const COLUMN_DEFINITIONS = [
  { id: 'created_at', title: 'Created At', sortable: true },
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: true },
  { id: 'summary', title: 'Summary', sortable: true },
  { id: 'owner', title: 'Owner', sortable: true },
];

const useCustomColumnRenderers = () => {
  const requirementsProvided = usePluginEntities('views.requires.provided');
  const customColumnRenderers: ColumnRenderers<View> = useMemo(() => ({
    title: {
      renderCell: (dashboard) => <TitleCell dashboard={dashboard} requirementsProvided={requirementsProvided} />,
    },
  }), [requirementsProvided]);

  return customColumnRenderers;
};

const DashboardsOverview = () => {
  const paginationQueryParameter = usePaginationQueryParameter(undefined, 20);
  const [visibleColumns, setVisibleColumns] = useState(INITIAL_COLUMNS);
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: paginationQueryParameter.page,
    pageSize: paginationQueryParameter.pageSize,
    query: '',
    sort: {
      columnId: 'title',
      order: 'asc',
    },
  });
  const columnRenderers = useCustomColumnRenderers();
  const { data: paginatedDashboards, refetch } = useDashboards(searchParams);

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setSearchParams((cur) => ({ ...cur, query: newQuery, page: 1 }));
  }, [paginationQueryParameter]);

  const onColumnsChange = useCallback((newVisibleColumns: Array<string>) => {
    setVisibleColumns(newVisibleColumns);
  }, []);

  const renderDashboardActions = useCallback((dashboard: View) => (
    <DashboardActions dashboard={dashboard} refetchDashboards={refetch} />
  ), [refetch]);

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

  if (!paginatedDashboards) {
    return <Spinner />;
  }

  const { list: dashboards, pagination } = paginatedDashboards;

  return (
    <PaginatedList onChange={onPageChange}
                   pageSize={searchParams.pageSize}
                   totalItems={pagination.total}>
      <div style={{ marginBottom: 15 }}>
        <SearchForm onSearch={onSearch}
                    queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                    onReset={onReset}
                    topMargin={0} />
      </div>
      {!dashboards?.length && (
        <Alert>
          {!searchParams.query ? (
            <>
              No dashboards have been created yet.
            </>
          ) : (
            'No dashboards have been found.'
          )}
        </Alert>
      )}
      {!!dashboards?.length && (
        <EntityDataTable<View> data={dashboards}
                               visibleColumns={visibleColumns}
                               onColumnsChange={onColumnsChange}
                               onSortChange={onSortChange}
                               activeSort={searchParams.sort}
                               rowActions={renderDashboardActions}
                               columnRenderers={columnRenderers}
                               columnDefinitions={COLUMN_DEFINITIONS} />
      )}
    </PaginatedList>
  );
};

export default DashboardsOverview;
