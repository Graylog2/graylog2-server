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
import { useState, useCallback, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { Alert, Button } from 'components/bootstrap';
import { PaginatedList, SearchForm, Spinner } from 'components/common';
import type View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import QueryHelper from 'components/common/QueryHelper';
import type { Sort, ColumnRenderers } from 'components/common/EntityDataTable';
import EntityDataTable from 'components/common/EntityDataTable';
import type { PaginatedViews } from 'views/stores/ViewManagementStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { SavedSearchesActions } from 'views/stores/SavedSearchesStore';
import type FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';

type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
  sort: Sort
}

const INITIAL_COLUMNS = ['title'];
const COLUMN_DEFINITIONS = [
  { id: 'created_at', title: 'Created At', sortable: true },
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: true },
  { id: 'summary', title: 'Summary', sortable: true },
  { id: 'owner', title: 'Owner', sortable: true },
];

const DEFAULT_PAGINATION = {
  query: '',
  page: 1,
  perPage: 10,
  count: 0,
};

const NoSavedSearches = styled(Alert)`
  clear: right;
  display: flex;
  align-items: center;
  margin-top: 15px;
`;

const onLoad = (onLoadSavedSearch: () => void, selectedSavedSearchId: string, loadFunc: (searchId: string) => void) => {
  if (!selectedSavedSearchId || !loadFunc) {
    return false;
  }

  loadFunc(selectedSavedSearchId);

  onLoadSavedSearch();

  return false;
};

const TitleLink = styled(Button)(({ $isActive }: { $isActive: boolean }) => css`
  font-weight: ${$isActive ? 'bold' : 'normal'};
  padding: 0;
`);

const customColumnRenderers = (onLoadSavedSearch: () => void, activeSavedSearchId: string): ColumnRenderers<View> => ({
  title: {
    renderCell: (search) => (
      <ViewLoaderContext.Consumer key={search.id}>
        {(loaderFunc) => (
          <TitleLink bsStyle="link"
                     onClick={() => onLoad(onLoadSavedSearch, search.id, loaderFunc)}
                     $isActive={search.id === activeSavedSearchId}>
            {search.title}
          </TitleLink>
        )}
      </ViewLoaderContext.Consumer>
    ),
  },
});

const onDelete = (e, savedSearch: View, deleteSavedSearch: (search: View) => void) => {
  e.stopPropagation();

  // eslint-disable-next-line no-alert
  if (window.confirm(`You are about to delete saved search: "${savedSearch.title}". Are you sure?`)) {
    deleteSavedSearch(savedSearch);
  }
};

const usePaginatedSavedSearches = (searchParams: SearchParams): {
  data: PaginatedViews | undefined,
  refetch: () => void,
  isFetching: boolean
} => {
  const { data, refetch, isFetching } = useQuery(
    ['saved-searches', 'overview', searchParams],
    () => SavedSearchesActions.search({
      query: searchParams.query,
      page: searchParams.page,
      perPage: searchParams.pageSize,
      sortBy: searchParams.sort.columnId,
      order: searchParams.sort.order,
    }),
    {
      onError: (error: FetchError) => {
        UserNotification.error(`Fetching saved searches failed with status: ${error}`,
          'Could not retrieve saved searches');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
    isFetching,
  });
};

const _updateListOnSearchDelete = (setSearchParams) => ViewManagementActions.delete.completed.listen(
  () => setSearchParams((cur) => ({ ...cur, page: DEFAULT_PAGINATION.page })),
);

type Props = {
  activeSavedSearchId: string,
  deleteSavedSearch: (view: View) => Promise<View>,
  onLoadSavedSearch: () => void,
};

const SavedSearchesList = ({
  activeSavedSearchId,
  deleteSavedSearch,
  onLoadSavedSearch,
}: Props) => {
  const [visibleColumns, setVisibleColumns] = useState(INITIAL_COLUMNS);
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: 1,
    pageSize: 10,
    query: '',
    sort: {
      columnId: 'title',
      order: 'asc',
    },
  });

  const { data, isFetching } = usePaginatedSavedSearches(searchParams);

  useEffect(() => _updateListOnSearchDelete(setSearchParams), []);

  const handleSearch = useCallback(
    (newQuery: string) => setSearchParams((cur) => ({
      ...cur,
      query: newQuery,
      page: DEFAULT_PAGINATION.page,
    })),
    [],
  );
  const handlePageSizeChange = useCallback(
    (newPage: number, newPerPage: number) => setSearchParams((cur) => ({
      ...cur,
      page: newPage,
      perPage: newPerPage,
    })),
    [],
  );
  const onSortChange = useCallback((newSort: Sort) => {
    setSearchParams((cur) => ({ ...cur, sort: newSort, page: 1 }));
  }, []);

  const onResetSearch = useCallback(() => handleSearch(''), [handleSearch]);
  const onColumnsChange = useCallback((newVisibleColumns: Array<string>) => {
    setVisibleColumns(newVisibleColumns);
  }, []);

  const renderSavedSearchActions = useCallback((search: View) => (
    <Button onClick={(e) => onDelete(e, search, deleteSavedSearch)}
            role="button"
            bsSize="xsmall"
            bsStyle="danger"
            title={`Delete search ${search.title}`}
            tabIndex={0}>
      Delete
    </Button>
  ), [deleteSavedSearch]);

  const columnRenderers = customColumnRenderers(onLoadSavedSearch, activeSavedSearchId);

  if (isFetching) {
    return <Spinner />;
  }

  const { list: savedSearches, pagination } = data;

  return (
    <PaginatedList onChange={handlePageSizeChange}
                   activePage={searchParams.page}
                   totalItems={pagination?.total}
                   pageSize={searchParams.pageSize}
                   useQueryParameter={false}>
      <SearchForm focusAfterMount
                  onSearch={handleSearch}
                  queryHelpComponent={<QueryHelper entityName="search" commonFields={['id', 'title']} />}
                  topMargin={0}
                  onReset={onResetSearch} />
      {pagination?.total === 0 && (
        <NoSavedSearches>
          No saved searches found.
        </NoSavedSearches>
      )}
      {!!savedSearches?.length && (
        <EntityDataTable<View> data={savedSearches}
                               visibleColumns={visibleColumns}
                               onColumnsChange={onColumnsChange}
                               onSortChange={onSortChange}
                               activeSort={searchParams.sort}
                               rowActions={renderSavedSearchActions}
                               columnRenderers={columnRenderers}
                               columnDefinitions={COLUMN_DEFINITIONS} />
      )}
    </PaginatedList>
  );
};

export default SavedSearchesList;
