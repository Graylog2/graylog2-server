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

import { isPermitted } from 'util/PermissionsMixin';
import { Alert } from 'components/bootstrap';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import ConfigurableDataTable from 'components/common/ConfigurableDataTable';
import StreamActions from 'components/streams/StreamActions';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import StreamStatusCell from 'components/streamrules/StreamStatusCell';
import type { CustomCells } from 'components/common/ConfigurableDataTable';

import CreateStreamButton from './CreateStreamButton';

const AVAILABLE_ATTRIBUTES = [
  { id: 'title', title: 'Title' },
  { id: 'description', title: 'Description' },
  { id: 'index_set_id', title: 'Index Set' },
  { id: 'disabled', title: 'Status' },
];

const VISIBLE_ATTRIBUTES = ['title', 'description', 'index_set_id', 'disabled'];

const customCells = (indexSets: Array<IndexSet>, userPermissions): CustomCells<Stream> => ({
  title: {
    renderCell: (stream) => (
      <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
    ),
  },
  index_set_id: {
    renderCell: (stream) => {
      if (!isPermitted(userPermissions, ['indexsets:read'])) {
        return null;
      }

      const indexSet = indexSets.find((is) => is.id === stream.index_set_id) || indexSets.find((is) => is.default);

      return (
        indexSet ? (
          <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
            {indexSet.title}
          </Link>
        ) : <i>not found</i>
      );
    },
  },
  disabled: {
    renderCell: (stream) => <StreamStatusCell stream={stream} />,
    width: '100px',
  },
});

type Props = {
  onStreamCreate: (stream: Stream) => Promise<void>,
  indexSets: Array<IndexSet>
}

const StreamsOverview = ({ onStreamCreate, indexSets }: Props) => {
  const currentUser = useCurrentUser();
  const paginationQueryParameter = usePaginationQueryParameter();
  const [searchParams, setSearchParams] = useState({
    page: paginationQueryParameter.page,
    perPage: paginationQueryParameter.pageSize,
    query: '',
  });
  const [streamRuleTypes, setStreamRuleTypes] = useState();
  const [listData, setListData] = useState<{
    streams: Array<Stream>,
    total: number
  }>({ streams: [], total: 0 });
  const { streams, total } = listData;

  const isLoading = !streams || !streamRuleTypes;

  const loadData = useCallback(() => {
    StreamsStore.searchPaginated(searchParams.page, searchParams.perPage, searchParams.query)
      .then(({ streams: newStreams, pagination: { total: newTotal } }) => {
        setListData({ streams: newStreams, total: newTotal });
      });
  }, [searchParams]);

  useEffect(() => {
    loadData();
  }, [loadData, searchParams]);

  useEffect(() => {
    StreamRulesStore.types().then((types) => {
      setStreamRuleTypes(types);
    });
  }, []);

  useEffect(() => {
    StreamsStore.onChange(loadData);
    StreamRulesStore.onChange(loadData);

    return () => {
      StreamsStore.unregister(loadData);
      StreamRulesStore.unregister(loadData);
    };
  }, [loadData]);

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

  if (isLoading) {
    return (
      <div style={{ marginLeft: 10 }}>
        <Spinner />
      </div>
    );
  }

  const renderStreamActions = (listItem: Stream) => <StreamActions stream={listItem} indexSets={indexSets} streamRuleTypes={streamRuleTypes} />;

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
                                   attributePermissions={{
                                     index_set_id: {
                                       permissions: ['indexsets:read'],
                                     },
                                   }}
                                   rowActions={renderStreamActions}
                                   customCells={customCells(indexSets, currentUser.permissions)}
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
