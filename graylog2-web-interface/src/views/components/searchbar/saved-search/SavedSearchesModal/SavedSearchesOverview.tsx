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
import { useCallback } from 'react';

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
import useSavedSearches from 'views/hooks/useSavedSearches';
import useColumnRenderers from 'views/components/searchbar/saved-search/useColumnRenderes';
import useSavedSearchPaginationAndTableLayout
  from 'views/components/searchbar/saved-search/useSavedSearchPaginationAndTableLayout';

import SearchActions from './SearchActions';

import BulkActions from '../BulkActions';
import { DEFAULT_LAYOUT } from '../Constants';

type Props = {
  activeSavedSearchId: string,
  deleteSavedSearch: (view: View) => Promise<void>,
  onLoadSavedSearch: () => void,
};

const SavedSearchesOverview = ({
  activeSavedSearchId,
  deleteSavedSearch,
  onLoadSavedSearch,
}: Props) => {
  const {
    isLoadingLayoutPreferences,
    onPageChange,
    layoutConfig,
    activePage,
    onSearch,
    onResetSearch,
    searchParams,
    onColumnsChange,
    onSortChange,
    onPageSizeChange,
  } = useSavedSearchPaginationAndTableLayout();

  const { data: paginatedSavedSearches, isInitialLoading: isLoadingSavedSearches, refetch } = useSavedSearches(searchParams, { enabled: !isLoadingLayoutPreferences });

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
        <EntityDataTable<View> entities={savedSearches}
                               visibleColumns={layoutConfig.displayedAttributes}
                               columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                               onColumnsChange={onColumnsChange}
                               bulkSelection={{ actions: <BulkActions /> }}
                               onSortChange={onSortChange}
                               activeSort={layoutConfig.sort}
                               entityAttributesAreCamelCase
                               pageSize={searchParams.pageSize}
                               onPageSizeChange={onPageSizeChange}
                               actionsCellWidth={120}
                               entityActions={renderSavedSearchActions}
                               columnRenderers={customColumnRenderers}
                               columnDefinitions={attributes} />
      )}
    </PaginatedList>
  );
};

export default SavedSearchesOverview;
