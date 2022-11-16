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

import { Alert } from 'components/bootstrap';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import type { Stream, StreamRuleType } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import ConfigurableDataTable from 'components/common/ConfigurableDataTable';
import StreamActions from 'components/streams/StreamActions';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { CustomCells } from 'components/common/ConfigurableDataTable';
import UserNotification from 'util/UserNotification';
import IndexSetCell from 'components/streams/StreamsOverview/IndexSetCell';

import StatusCell from './StatusCell';

import CreateStreamButton from '../CreateStreamButton';

const AVAILABLE_ATTRIBUTES = [
  { id: 'title', title: 'Title' },
  { id: 'description', title: 'Description' },
  { id: 'index_set_id', title: 'Index Set' },
  { id: 'disabled', title: 'Status' },
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
}

const customCells = (indexSets: Array<IndexSet>): CustomCells<Stream> => ({
  title: {
    renderCell: (stream) => (
      <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
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
    () => StreamsStore.searchPaginated(searchParams.page, searchParams.perPage, searchParams.query),
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

const useStreamRuleTypes = (): { data: { types: Array<StreamRuleType> } | undefined } => {
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
  });
  const { data: streamRulesData } = useStreamRuleTypes();
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

  const renderStreamActions = useCallback((listItem: Stream) => (
    <StreamActions stream={listItem}
                   indexSets={indexSets}
                   streamRuleTypes={streamRulesData?.types} />
  ), [indexSets, streamRulesData?.types]);

  if (!paginatedStreams || !streamRulesData) {
    return (<Spinner />);
  }

  const { streams, pagination: { total } } = paginatedStreams;

  return (
    <PaginatedList onChange={onPageChange}
                   totalItems={total}>
      <div style={{ marginBottom: 15 }}>
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
                                   attributes={VISIBLE_ATTRIBUTES}
                                   attributePermissions={ATTRIBUTE_PERMISSIONS}
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
