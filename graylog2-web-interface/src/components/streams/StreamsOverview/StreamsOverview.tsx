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
import PropTypes from 'prop-types';
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { PaginatedList, SearchForm, NoSearchResult } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import EntityDataTable from 'components/common/EntityDataTable';
import useStreams from 'components/streams/hooks/useStreams';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import {
  DEFAULT_LAYOUT,
  ENTITY_TABLE_ID,
  ADDITIONAL_ATTRIBUTES,
} from 'components/streams/StreamsOverview/Constants';
import EntityFilters from 'components/common/EntityFilters';
import type { Filters } from 'components/common/EntityFilters/types';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import useTableElements from 'components/streams/StreamsOverview/hooks/useTableComponents';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';

import CustomColumnRenderers from './ColumnRenderers';
import useTableEventHandlers from './hooks/useTableEventHandlers';

const useRefetchStreamsOnStoreChange = (refetchStreams: () => void) => {
  useEffect(() => {
    StreamsStore.onChange(() => refetchStreams());
    StreamRulesStore.onChange(() => refetchStreams());

    return () => {
      StreamsStore.unregister(() => refetchStreams());
      StreamRulesStore.unregister(() => refetchStreams());
    };
  }, [refetchStreams]);
};

type Props = {
  indexSets: Array<IndexSet>
}

const StreamsOverview = ({ indexSets }: Props) => {
  const queryClient = useQueryClient();
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setQuery] = useState('');
  const { layoutConfig, isLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const { data: paginatedStreams, isInitialLoading: isLoadingStreams, refetch: refetchStreams } = useStreams({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
    filters: urlQueryFilters,
  }, { enabled: !isLoadingLayoutPreferences });

  const { data: filtersWithTitle, onChange: onChangeFiltersWithTitle } = useFiltersWithTitle(urlQueryFilters, paginatedStreams?.attributes, !!paginatedStreams);

  const { entityActions, expandedSections, bulkActions } = useTableElements({ indexSets });
  const {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  } = useTableEventHandlers({
    paginationQueryParameter,
    updateTableLayout,
    setQuery,
  });

  useRefetchStreamsOnStoreChange(refetchStreams);

  const columnRenderers = useMemo(() => CustomColumnRenderers(indexSets), [indexSets]);
  const columnDefinitions = useMemo(
    () => ([...(paginatedStreams?.attributes ?? []), ...ADDITIONAL_ATTRIBUTES]),
    [paginatedStreams?.attributes],
  );
  const onChangeFilters = useCallback((newFilters: Filters) => {
    const newUrlQueryFilters = Object.entries(newFilters).reduce((col, [attributeId, filterCol]) => ({
      ...col,
      [attributeId]: [...col[attributeId] ?? [], ...filterCol.map(({ value }) => value)],
    }), {});

    onChangeFiltersWithTitle(newFilters, newUrlQueryFilters);

    setUrlQueryFilters(newUrlQueryFilters);
  }, [paginatedStreams?.attributes, queryClient, setUrlQueryFilters]);

  if (isLoadingLayoutPreferences || isLoadingStreams) {
    return <Spinner />;
  }

  const { elements, attributes, pagination: { total } } = paginatedStreams;

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    queryHelpComponent={<QueryHelper entityName="stream" />}>
          <EntityFilters attributes={attributes}
                         onChangeFilters={onChangeFilters}
                         activeFilters={filtersWithTitle}
                         filterValueRenderers={FilterValueRenderers} />
        </SearchForm>
      </div>
      <div>
        {elements?.length === 0 ? (
          <NoSearchResult>No streams have been found</NoSearchResult>
        ) : (
          <EntityDataTable<Stream> data={elements}
                                   visibleColumns={layoutConfig.displayedAttributes}
                                   columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                   onColumnsChange={onColumnsChange}
                                   onSortChange={onSortChange}
                                   onPageSizeChange={onPageSizeChange}
                                   pageSize={layoutConfig.pageSize}
                                   bulkActions={bulkActions}
                                   expandedSectionsRenderer={expandedSections}
                                   activeSort={layoutConfig.sort}
                                   rowActions={entityActions}
                                   columnRenderers={columnRenderers}
                                   columnDefinitions={columnDefinitions}
                                   entityAttributesAreCamelCase={false} />
        )}
      </div>
    </PaginatedList>
  );
};

StreamsOverview.propTypes = {
  indexSets: PropTypes.array.isRequired,
};

export default StreamsOverview;
