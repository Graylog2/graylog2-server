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
import { useState, useCallback, useMemo } from 'react';

import { Button } from 'components/bootstrap';
import { PaginatedList, SearchForm, Spinner, NoEntitiesExist, NoSearchResult } from 'components/common';
import type View from 'views/logic/views/View';
import QueryHelper from 'components/common/QueryHelper';
import EntityDataTable from 'components/common/EntityDataTable';
import type { Sort } from 'stores/PaginationTypes';
import useSavedSearches from 'views/hooks/useSavedSearches';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useColumnRenderers from 'views/components/searchbar/saved-search/useColumnRenderes';

import BulkActions from './BulkActions';
import { ENTITY_TABLE_ID, DEFAULT_LAYOUT } from './Constants';

const onDelete = (e, savedSearch: View, deleteSavedSearch: (search: View) => Promise<View>, activeSavedSearchId: string, refetch: () => void) => {
  e.stopPropagation();

  // eslint-disable-next-line no-alert
  if (window.confirm(`You are about to delete saved search: "${savedSearch.title}". Are you sure?`)) {
    deleteSavedSearch(savedSearch).then(() => {
      if (savedSearch.id !== activeSavedSearchId) {
        refetch();
      }
    });
  }
};

const renderBulkActions = (
  selectedSavedSearchIds: Array<string>,
  setSelectedSavedSearchIds: (streamIds: Array<string>) => void,
) => (
  <BulkActions selectedSavedSearchIds={selectedSavedSearchIds}
               setSelectedSavedSearchIds={setSelectedSavedSearchIds} />
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
  const [query, setQuery] = useState('');
  const [activePage, setActivePage] = useState(1);
  const { layoutConfig, isLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const searchParams = useMemo(() => ({
    query,
    page: activePage,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }), [activePage, layoutConfig.pageSize, layoutConfig.sort, query]);

  const { data: paginatedSavedSearches, isLoading, refetch } = useSavedSearches(searchParams, { enabled: !isLoadingLayoutPreferences });
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);

  const onPageChange = useCallback(
    (newPage: number, newPageSize: number) => {
      if (newPage) {
        setActivePage(newPage);
      }

      if (newPageSize) {
        updateTableLayout({ perPage: newPageSize });
      }
    }, [updateTableLayout],
  );

  const onSortChange = useCallback((newSort: Sort) => {
    setActivePage(1);
    updateTableLayout({ sort: newSort });
  }, [updateTableLayout]);

  const onSearch = useCallback((newQuery: string) => {
    setActivePage(1);
    setQuery(newQuery);
  }, []);

  const onResetSearch = useCallback(() => onSearch(''), [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const renderSavedSearchActions = useCallback((search: View) => (
    <Button onClick={(e) => onDelete(e, search, deleteSavedSearch, activeSavedSearchId, refetch)}
            role="button"
            bsSize="xsmall"
            bsStyle="danger"
            title={`Delete search ${search.title}`}
            tabIndex={0}>
      Delete
    </Button>
  ), [activeSavedSearchId, deleteSavedSearch, refetch]);

  const customColumnRenderers = useColumnRenderers(onLoadSavedSearch, searchParams);

  if (isLoading) {
    return <Spinner />;
  }

  const { list: savedSearches, pagination, attributes } = paginatedSavedSearches;

  return (
    <PaginatedList onChange={onPageChange}
                   totalItems={pagination?.total}
                   pageSize={layoutConfig.pageSize}
                   activePage={activePage}
                   useQueryParameter={false}>
      <div style={{ marginBottom: '5px' }}>
        <SearchForm focusAfterMount
                    onSearch={onSearch}
                    queryHelpComponent={<QueryHelper entityName="search" commonFields={['id', 'title']} />}
                    topMargin={0}
                    onReset={onResetSearch} />
      </div>
      {pagination?.total === 0 && !searchParams.query && (
        <NoEntitiesExist>
          No saved searches have been created yet.
        </NoEntitiesExist>
      )}
      {pagination?.total === 0 && searchParams.query && (
        <NoSearchResult>
          No saved searches found.
        </NoSearchResult>
      )}
      {!!savedSearches?.length && (
        <EntityDataTable<View> data={savedSearches}
                               visibleColumns={layoutConfig.displayedAttributes}
                               columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                               onColumnsChange={onColumnsChange}
                               bulkActions={renderBulkActions}
                               onSortChange={onSortChange}
                               activeSort={layoutConfig.sort}
                               rowActions={renderSavedSearchActions}
                               columnRenderers={customColumnRenderers}
                               columnDefinitions={attributes} />
      )}
    </PaginatedList>
  );
};

export default SavedSearchesList;
