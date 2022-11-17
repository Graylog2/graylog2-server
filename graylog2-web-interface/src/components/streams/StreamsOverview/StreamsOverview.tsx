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
import React, { useState, useEffect, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import styled from 'styled-components';

import { Alert, Label } from 'components/bootstrap';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import type { Stream, StreamRuleType } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import ConfigurableDataTable from 'components/common/ConfigurableDataTable';
import StreamActions from 'components/streams/StreamsOverview/StreamActions';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { CustomCells, Sort } from 'components/common/ConfigurableDataTable';
import UserNotification from 'util/UserNotification';
import IndexSetCell from 'components/streams/StreamsOverview/IndexSetCell';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';

import StatusCell from './StatusCell';

import CreateStreamButton from '../CreateStreamButton';

const DefaultLabel = styled(Label)`
  display: inline-flex;
  margin-left: 5px;
  vertical-align: inherit;
`;

const AVAILABLE_ATTRIBUTES = [
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: true },
  { id: 'index_set_id', title: 'Index Set', sortable: true },
  { id: 'disabled', title: 'Status', sortable: true },
];

const ATTRIBUTE_PERMISSIONS = {
  index_set_id: {
    permissions: ['indexsets:read'],
  },
};

const VISIBLE_ATTRIBUTES = ['title', 'description', 'index_set_id', 'disabled'];

type SearchParams = {
  page: number,
  perPage: number,
  query: string,
  sort: Sort
}

const customCells = (indexSets: Array<IndexSet>): CustomCells<Stream> => ({
  title: {
    renderCell: (stream) => (
      <>
        <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
        {stream.is_default && <DefaultLabel bsStyle="primary" bsSize="xsmall">Default</DefaultLabel>}
      </>
    ),
  },
  index_set_id: {
    renderCell: (stream) => <IndexSetCell indexSets={indexSets} stream={stream} />,
  },
  disabled: {
    renderCell: (stream) => <StatusCell stream={stream} />,
    width: '100px',
  },
});

const usePaginatedStreams = (searchParams: SearchParams): { data: { streams: Array<Stream>, pagination: { total: number } } | undefined, refetch: () => void } => {
  const { data, refetch } = useQuery(
    ['streams', 'overview', searchParams],
    () => StreamsStore.searchPaginated(
      searchParams.page,
      searchParams.perPage,
      searchParams.query,
      { sort: searchParams?.sort.attributeId, order: searchParams?.sort.order },
    ),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
  });
};

const useStreamRuleTypes = (): { data: Array<StreamRuleType> | undefined } => {
  const { data } = useQuery(
    ['streams', 'rule-types'],
    () => StreamRulesStore.types(),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading stream rule types failed with status: ${errorThrown}`,
          'Could not load stream rule types');
      },
      keepPreviousData: true,
    },
  );

  return ({ data });
};

type Props = {
  onStreamCreate: (stream: Stream) => Promise<void>,
  indexSets: Array<IndexSet>
}

const StreamsOverview = ({ onStreamCreate, indexSets }: Props) => {
  const paginationQueryParameter = usePaginationQueryParameter();

  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: paginationQueryParameter.page,
    perPage: paginationQueryParameter.pageSize,
    query: '',
    sort: {
      attributeId: 'title',
      order: 'asc',
    },
  });
  const { data: streamRuleTypes } = useStreamRuleTypes();
  const { data: paginatedStreams, refetch: refetchStreams } = usePaginatedStreams(searchParams);

  useEffect(() => {
    StreamsStore.onChange(() => refetchStreams());
    StreamRulesStore.onChange(() => refetchStreams());

    return () => {
      StreamsStore.unregister(() => refetchStreams());
      StreamRulesStore.unregister(() => refetchStreams());
    };
  }, [refetchStreams]);

  const onPageChange = useCallback(
    (newPage: number, newPerPage: number) => setSearchParams((cur) => ({ ...cur, page: newPage, perPage: newPerPage })),
    [],
  );

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setSearchParams((cur) => ({ ...cur, query: newQuery }));
  }, [paginationQueryParameter]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onSortChange = useCallback((newSort: Sort) => {
    setSearchParams((cur) => ({ ...cur, sort: newSort }));
  }, []);

  const renderStreamActions = useCallback((listItem: Stream) => (
    <StreamActions stream={listItem}
                   indexSets={indexSets}
                   streamRuleTypes={streamRuleTypes} />
  ), [indexSets, streamRuleTypes]);

  const renderBulkActions = (
    selectedStreamIds: Array<string>,
    setSelectedStreamIds: (streamIds: Array<string>) => void,
  ) => (
    <BulkActions selectedStreamIds={selectedStreamIds} setSelectedStreamIds={setSelectedStreamIds} />
  );

  if (!paginatedStreams || !streamRuleTypes) {
    return (<Spinner />);
  }

  const { streams, pagination: { total } } = paginatedStreams;

  return (
    <PaginatedList onChange={onPageChange}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onReset}
                    queryHelpComponent={<QueryHelper entityName="stream" />} />
      </div>
      <div>
        {streams?.length === 0
          ? (
            <Alert bsStyle="warning">
              <Icon name="info-circle" />&nbsp;No streams found.
              <IfPermitted permissions="streams:create">
                <CreateStreamButton bsSize="small"
                                    bsStyle="link"
                                    className="btn-text"
                                    buttonText="Create one now"
                                    indexSets={indexSets}
                                    onCreate={onStreamCreate} />
              </IfPermitted>
            </Alert>
          )
          : (
            <ConfigurableDataTable rows={streams}
                                   total={total}
                                   attributes={VISIBLE_ATTRIBUTES}
                                   attributePermissions={ATTRIBUTE_PERMISSIONS}
                                   onSortChange={onSortChange}
                                   bulkActions={renderBulkActions}
                                   activeSort={searchParams.sort}
                                   rowActions={renderStreamActions}
                                   customCells={customCells(indexSets)}
                                   availableAttributes={AVAILABLE_ATTRIBUTES} />
          )}
      </div>
    </PaginatedList>
  );
};

StreamsOverview.propTypes = {
  onStreamCreate: PropTypes.func.isRequired,
  indexSets: PropTypes.array.isRequired,
};

export default StreamsOverview;
