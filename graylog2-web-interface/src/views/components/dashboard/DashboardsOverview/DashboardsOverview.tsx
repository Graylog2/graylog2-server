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
import type { QueryClient } from '@tanstack/react-query';
import { useQueryClient } from '@tanstack/react-query';
import type { SearchParams, Sort } from 'src/stores/PaginationTypes';

import { PaginatedList, SearchForm, Spinner, NoSearchResult, NoEntitiesExist } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import EntityDataTable from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import usePluginEntities from 'hooks/usePluginEntities';
import DashboardActions from 'views/components/dashboard/DashboardsOverview/DashboardActions';
import FavoriteIcon from 'views/components/FavoriteIcon';

import BulkActions from './BulkActions';
import TitleCell from './TitleCell';

const INITIAL_COLUMNS = ['title', 'description', 'summary', 'favorite'];
const COLUMNS_ORDER = ['title', 'summary', 'description', 'owner', 'created_at', 'favorite'];

const useCustomColumnRenderers = ({ queryClient, searchParams }: { queryClient: QueryClient, searchParams: SearchParams
}) => {
  const requirementsProvided = usePluginEntities('views.requires.provided');
  const customColumnRenderers: ColumnRenderers<View> = useMemo(() => ({
    title: {
      renderCell: (dashboard) => <TitleCell dashboard={dashboard} requirementsProvided={requirementsProvided} />,
    },
    favorite: {
      renderCell: (search) => (
        <FavoriteIcon isFavorite={search.favorite}
                      id={search.id}
                      onChange={(newValue) => {
                        queryClient.setQueriesData(['dashboards', 'overview', searchParams], (cur: {
                          list: Readonly<Array<View>>,
                          pagination: { total: number }
                        }) => ({
                          ...cur,
                          list: cur.list.map((view) => {
                            if (view.id === search.id) {
                              return view.toBuilder().favorite(newValue).build();
                            }

                            return view;
                          }),
                        }
                        ));
                      }} />
      ),
    },
  }), [queryClient, requirementsProvided, searchParams]);

  return customColumnRenderers;
};

const renderBulkActions = (
  selectedDashboardIds: Array<string>,
  setSelectedDashboardIds: (streamIds: Array<string>) => void,
) => (
  <BulkActions selectedDashboardIds={selectedDashboardIds}
               setSelectedDashboardIds={setSelectedDashboardIds} />
);

const DashboardsOverview = () => {
  const paginationQueryParameter = usePaginationQueryParameter(undefined, 20);
  const [visibleColumns, setVisibleColumns] = useState(INITIAL_COLUMNS);
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: paginationQueryParameter.page,
    pageSize: paginationQueryParameter.pageSize,
    query: '',
    sort: {
      attributeId: 'title',
      direction: 'asc',
    },
  });
  const columnRenderers = useCustomColumnRenderers({ queryClient, searchParams });
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

  const { list: dashboards, pagination, attributes } = paginatedDashboards;

  return (
    <PaginatedList onChange={onPageChange}
                   pageSize={searchParams.pageSize}
                   totalItems={pagination.total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                    onReset={onReset}
                    topMargin={0} />
      </div>
      {!dashboards?.length && !searchParams.query && (
        <NoEntitiesExist>
          No dashboards have been created yet.
        </NoEntitiesExist>
      )}
      {!dashboards?.length && searchParams.query && (
        <NoSearchResult>No dashboards have been found.</NoSearchResult>
      )}
      {!!dashboards?.length && (
        <EntityDataTable<View> data={dashboards}
                               visibleColumns={visibleColumns}
                               onColumnsChange={onColumnsChange}
                               onSortChange={onSortChange}
                               activeSort={searchParams.sort}
                               rowActions={renderDashboardActions}
                               bulkActions={renderBulkActions}
                               columnRenderers={columnRenderers}
                               columnsOrder={COLUMNS_ORDER}
                               columnDefinitions={attributes} />
      )}
    </PaginatedList>
  );
};

export default DashboardsOverview;
