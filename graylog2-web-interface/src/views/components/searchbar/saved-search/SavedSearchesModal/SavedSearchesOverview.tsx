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

import {
  PaginatedList,
  SearchForm,
  Spinner,
  NoEntitiesExist,
  NoSearchResult,
} from 'components/common';
import type View from 'views/logic/views/View';
import QueryHelper from 'components/common/QueryHelper';
import EntityDataTable from 'components/common/EntityDataTable';
import type { Sort } from 'stores/PaginationTypes';
import useSavedSearches from 'views/hooks/useSavedSearches';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useColumnRenderers from 'views/components/searchbar/saved-search/useColumnRenderes';

import SearchActions from './SearchActions';

import BulkActions from '../BulkActions';
import { ENTITY_TABLE_ID, DEFAULT_LAYOUT } from '../Constants';

type Props = {
  activeSavedSearchId: string,
  deleteSavedSearch: (view: View) => Promise<View>,
  onLoadSavedSearch: () => void,
};

const SavedSearchesOverview = ({
  activeSavedSearchId,
  deleteSavedSearch,
  onLoadSavedSearch,
}: Props) => {
  const [query, setQuery] = useState('');
  const [activePage, setActivePage] = useState(1);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
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

  const { data: paginatedSavedSearches, isInitialLoading: isLoadingSavedSearches, refetch } = useSavedSearches(searchParams, { enabled: !isLoadingLayoutPreferences });
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

  const onPageSizeChange = useCallback((newPageSize: number) => {
    setActivePage(1);
    updateTableLayout({ perPage: newPageSize });
  }, [updateTableLayout]);

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
    <SearchActions search={search}
                   onDeleteSavedSearch={deleteSavedSearch}
                   refetch={refetch}
                   activeSavedSearchId={activeSavedSearchId} />
  ), [activeSavedSearchId, deleteSavedSearch, refetch]);

  const customColumnRenderers = useColumnRenderers(onLoadSavedSearch, searchParams);

  if (isLoadingSavedSearches || isLoadingLayoutPreferences) {
    return <Spinner />;
  }

  const { list: savedSearches, pagination, attributes } = paginatedSavedSearches;

  return (
    <PaginatedList onChange={onPageChange}
                   totalItems={pagination?.total}
                   pageSize={layoutConfig.pageSize}
                   activePage={activePage}
                   showPageSizeSelect={false}
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
          No saved searches have been found.
        </NoSearchResult>
      )}
      {!!savedSearches?.length && (
        <EntityDataTable<View> data={savedSearches}
                               visibleColumns={layoutConfig.displayedAttributes}
                               columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                               onColumnsChange={onColumnsChange}
                               bulkSelection={{ actions: <BulkActions /> }}
                               onSortChange={onSortChange}
                               activeSort={layoutConfig.sort}
                               pageSize={searchParams.pageSize}
                               onPageSizeChange={onPageSizeChange}
                               actionsCellWidth={120}
                               rowActions={renderSavedSearchActions}
                               columnRenderers={customColumnRenderers}
                               columnDefinitions={attributes} />
      )}
    </PaginatedList>
  );
};

export default SavedSearchesOverview;
