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
import styled from 'styled-components';

import { Label } from 'components/bootstrap';
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
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import IndexSetCell from 'components/streams/StreamsOverview/IndexSetCell';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import ThroughputCell from 'components/streams/StreamsOverview/ThroughputCell';
import type { Sort } from 'stores/PaginationTypes';
import useStreams from 'components/streams/hooks/useStreams';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';

import StatusCell from './StatusCell';

const ENTITY_TABLE_ID = 'streams';
const DEFAULT_PAGE_SIZE = 20;

const DEFAULT_SORT = {
  attributeId: 'title',
  direction: 'asc',
} as const;

const DefaultLabel = styled(Label)`
  display: inline-flex;
  margin-left: 5px;
  vertical-align: inherit;
`;

const CUSTOM_COLUMN_DEFINITIONS = [
  { id: 'index_set_title', title: 'Index Set', sortable: true, permissions: ['indexsets:read'] },
  { id: 'throughput', title: 'Throughput' },
];

const INITIAL_DISPLAYED_ATTRIBUTES = ['title', 'description', 'index_set_title', 'throughput', 'disabled'];
const COLUMNS_ORDER = ['title', 'description', 'index_set_title', 'throughput', 'disabled', 'created_at'];

const customColumnRenderers = (indexSets: Array<IndexSet>): ColumnRenderers<Stream> => ({
  title: {
    renderCell: (stream) => (
      <>
        <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
        {stream.is_default && <DefaultLabel bsStyle="primary" bsSize="xsmall">Default</DefaultLabel>}
      </>
    ),
  },
  index_set_title: {
    renderCell: (stream) => <IndexSetCell indexSets={indexSets} stream={stream} />,
    width: 0.7,
  },
  throughput: {
    renderCell: (stream) => <ThroughputCell stream={stream} />,
    staticWidth: 120,
  },
  disabled: {
    renderCell: (stream) => <StatusCell stream={stream} />,
    staticWidth: 100,
  },
});

type Props = {
  indexSets: Array<IndexSet>
}

const StreamsOverview = ({ indexSets }: Props) => {
  const paginationQueryParameter = usePaginationQueryParameter(undefined, DEFAULT_PAGE_SIZE);
  const [query, setQuery] = useState('');
  const { data: streamRuleTypes } = useStreamRuleTypes();
  const { layoutConfig, isLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: paginationQueryParameter.pageSize,
    defaultDisplayedAttributes: INITIAL_DISPLAYED_ATTRIBUTES,
    defaultSort: DEFAULT_SORT,
  });
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const { data: paginatedStreams, refetch: refetchStreams } = useStreams({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }, { enabled: !isLoadingLayoutPreferences });

  const columnRenderers = useMemo(() => customColumnRenderers(indexSets), [indexSets]);
  const columnDefinitions = useMemo(
    () => ([...(paginatedStreams?.attributes ?? []), ...CUSTOM_COLUMN_DEFINITIONS]),
    [paginatedStreams?.attributes],
  );

  useEffect(() => {
    StreamsStore.onChange(() => refetchStreams());
    StreamRulesStore.onChange(() => refetchStreams());

    return () => {
      StreamsStore.unregister(() => refetchStreams());
      StreamRulesStore.unregister(() => refetchStreams());
    };
  }, [refetchStreams]);

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
                   indexSets={indexSets}
                   streamRuleTypes={streamRuleTypes} />
  ), [indexSets, streamRuleTypes]);

  const renderBulkActions = useCallback((
    selectedStreamIds: Array<string>,
    setSelectedStreamIds: (streamIds: Array<string>) => void,
  ) => (
    <BulkActions selectedStreamIds={selectedStreamIds}
                 setSelectedStreamIds={setSelectedStreamIds}
                 indexSets={indexSets} />
  ), [indexSets]);

  if (!paginatedStreams || !streamRuleTypes) {
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
                                   columnsOrder={COLUMNS_ORDER}
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
