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

import { PaginatedList, SearchForm, NoSearchResult } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import EntityDataTable from 'components/common/EntityDataTable';
import StreamActions from 'components/streams/StreamsOverview/StreamActions';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import type { Sort } from 'stores/PaginationTypes';
import useStreams from 'components/streams/hooks/useStreams';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import {
  DEFAULT_LAYOUT,
  ENTITY_TABLE_ID,
  ADDITIONAL_ATTRIBUTES,
} from 'components/streams/StreamsOverview/Constants';

import CustomColumnRenderers from './ColumnRenderers';

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
  const [query, setQuery] = useState('');
  const paginationQueryParameter = usePaginationQueryParameter(undefined, DEFAULT_LAYOUT.pageSize);
  const { layoutConfig, isLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: paginationQueryParameter.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const { data: paginatedStreams, refetch: refetchStreams } = useStreams({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }, { enabled: !isLoadingLayoutPreferences });

  useRefetchStreamsOnStoreChange(refetchStreams);

  const columnRenderers = useMemo(() => CustomColumnRenderers(indexSets), [indexSets]);
  const columnDefinitions = useMemo(
    () => ([...(paginatedStreams?.attributes ?? []), ...ADDITIONAL_ATTRIBUTES]),
    [paginatedStreams?.attributes],
  );

  const onPageChange = useCallback((_newPage: number, newPageSize: number) => {
    if (newPageSize) {
      updateTableLayout({ perPage: newPageSize });
    }
  }, [updateTableLayout]);

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setQuery(newQuery);
  }, [paginationQueryParameter]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    updateTableLayout({ sort: newSort });
    paginationQueryParameter.resetPage();
  }, [paginationQueryParameter, updateTableLayout]);

  const renderStreamActions = useCallback((listItem: Stream) => (
    <StreamActions stream={listItem}
                   indexSets={indexSets} />
  ), [indexSets]);

  const renderBulkActions = useCallback((
    selectedStreamIds: Array<string>,
    setSelectedStreamIds: (streamIds: Array<string>) => void,
  ) => (
    <BulkActions selectedStreamIds={selectedStreamIds}
                 setSelectedStreamIds={setSelectedStreamIds}
                 indexSets={indexSets} />
  ), [indexSets]);

  if (!paginatedStreams) {
    return <Spinner />;
  }

  const { elements, pagination: { total } } = paginatedStreams;

  return (
    <PaginatedList onChange={onPageChange}
                   pageSize={layoutConfig.pageSize}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onReset}
                    queryHelpComponent={<QueryHelper entityName="stream" />} />
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
                                   bulkActions={renderBulkActions}
                                   activeSort={layoutConfig.sort}
                                   rowActions={renderStreamActions}
                                   columnRenderers={columnRenderers}
                                   columnDefinitions={columnDefinitions} />
        )}
      </div>
    </PaginatedList>
  );
};

StreamsOverview.propTypes = {
  indexSets: PropTypes.array.isRequired,
};

export default StreamsOverview;
