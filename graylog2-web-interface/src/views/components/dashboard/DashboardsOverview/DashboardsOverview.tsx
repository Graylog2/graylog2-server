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
import React, { useCallback, useMemo } from 'react';
import { useQueryParam, StringParam } from 'use-query-params';

import type { Sort } from 'stores/PaginationTypes';
import { PaginatedList, SearchForm, Spinner, NoSearchResult, NoEntitiesExist } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';
import EntityDataTable from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import DashboardActions from 'views/components/dashboard/DashboardsOverview/DashboardActions';
import useColumnRenderers from 'views/components/dashboard/DashboardsOverview/useColumnRenderers';
import { DEFAULT_LAYOUT, ENTITY_TABLE_ID } from 'views/components/dashboard/DashboardsOverview/Constants';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';

import BulkActions from './BulkActions';

const renderBulkActions = (
  selectedDashboardIds: Array<string>,
  setSelectedDashboardIds: (streamIds: Array<string>) => void,
) => (
  <BulkActions selectedDashboardIds={selectedDashboardIds}
               setSelectedDashboardIds={setSelectedDashboardIds} />
);

const DashboardsOverview = () => {
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const searchParams = useMemo(() => ({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }), [layoutConfig.pageSize,
    layoutConfig.sort,
    paginationQueryParameter.page,
    query,
  ]);
  const customColumnRenderers = useColumnRenderers({ searchParams });
  const { data: paginatedDashboards, isInitialLoading: isLoadingDashboards, refetch } = useDashboards(searchParams, { enabled: !isLoadingLayoutPreferences });
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setQuery(newQuery);
  }, [paginationQueryParameter, setQuery]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const renderDashboardActions = useCallback((dashboard: View) => (
    <DashboardActions dashboard={dashboard} refetchDashboards={refetch} />
  ), [refetch]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onPageSizeChange = (newPageSize: number) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ perPage: newPageSize });
  };

  const onSortChange = useCallback((newSort: Sort) => {
    updateTableLayout({ sort: newSort });
    paginationQueryParameter.resetPage();
  }, [paginationQueryParameter, updateTableLayout]);

  if (isLoadingDashboards || isLoadingLayoutPreferences) {
    return <Spinner />;
  }

  const { list: dashboards, pagination, attributes } = paginatedDashboards;

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={pagination.total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                    onReset={onReset}
                    query={query}
                    topMargin={0} />
      </div>
      {!dashboards?.length && !query && (
        <NoEntitiesExist>
          No dashboards have been created yet.
        </NoEntitiesExist>
      )}
      {!dashboards?.length && query && (
        <NoSearchResult>No dashboards have been found.</NoSearchResult>
      )}
      {!!dashboards?.length && (
        <EntityDataTable<View> activeSort={layoutConfig.sort}
                               bulkActions={renderBulkActions}
                               columnDefinitions={attributes}
                               columnRenderers={customColumnRenderers}
                               columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                               data={dashboards}
                               onColumnsChange={onColumnsChange}
                               onPageSizeChange={onPageSizeChange}
                               onSortChange={onSortChange}
                               pageSize={layoutConfig.pageSize}
                               rowActions={renderDashboardActions}
                               actionsCellWidth={160}
                               visibleColumns={layoutConfig.displayedAttributes} />
      )}
    </PaginatedList>
  );
};

export default DashboardsOverview;
