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

import { Alert } from 'components/bootstrap';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import useCurrentUser from 'hooks/useCurrentUser';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import StreamList from './StreamList';
import CreateStreamButton from './CreateStreamButton';

const StreamComponent = ({ onStreamSave, indexSets }) => {
  const currentUser = useCurrentUser();
  const paginationQueryParameter = usePaginationQueryParameter();
  const [pagination, setPagination] = useState({
    count: 0,
    total: 0,
    query: '',
  });
  const [streamRuleTypes, setStreamRuleTypes] = useState();
  const [streams, setStreams] = useState<Array<any>>();

  const loadData = useCallback((callback?: () => void, page: number = paginationQueryParameter.page, perPage: number = paginationQueryParameter.pageSize) => {
    StreamsStore.searchPaginated(page, perPage, pagination.query)
      .then(({ streams: newStreams, pagination: newPagination }) => {
        setStreams(newStreams);
        setPagination(newPagination);
      })
      .then(() => {
        if (callback) {
          callback();
        }
      });
  }, [pagination.query, paginationQueryParameter.page, paginationQueryParameter.pageSize]);

  useEffect(() => {
    loadData();

    StreamRulesStore.types().then((types) => {
      setStreamRuleTypes(types);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    StreamsStore.onChange(loadData);
    StreamRulesStore.onChange(loadData);

    return () => {
      StreamsStore.unregister(loadData);
      StreamRulesStore.unregister(loadData);
    };
  }, [loadData]);

  const isLoading = !(streams && streamRuleTypes);

  const _onPageChange = useCallback((newPage, newPerPage) => loadData(null, newPage, newPerPage), [loadData]);

  const _onSearch = useCallback((query, resetLoadingCallback) => {
    const newPagination = { ...pagination, query: query };
    paginationQueryParameter.resetPage();
    setPagination(newPagination);
    loadData(resetLoadingCallback);
  }, [loadData, pagination, paginationQueryParameter]);

  const _onReset = useCallback(() => {
    const newPagination = { ...pagination, query: '' };
    paginationQueryParameter.resetPage();
    setPagination(newPagination);
    loadData();
  }, [loadData, pagination, paginationQueryParameter]);

  if (isLoading) {
    return (
      <div style={{ marginLeft: 10 }}>
        <Spinner />
      </div>
    );
  }

  return (
    <PaginatedList onChange={_onPageChange}
                   totalItems={pagination.total}>
      <div style={{ marginBottom: 15 }}>
        <SearchForm onSearch={_onSearch}
                    onReset={_onReset}
                    queryHelpComponent={<QueryHelper entityName="stream" />}
                    useLoadingState />
      </div>
      <div>{streams?.length === 0
        ? (
          <Alert bsStyle="warning">
            <Icon name="info-circle" />&nbsp;No streams found.
            <IfPermitted permissions="streams:create">
              <CreateStreamButton bsSize="small"
                                  bsStyle="link"
                                  className="btn-text"
                                  buttonText="Create one now"
                                  indexSets={indexSets}
                                  onSave={onStreamSave} />
            </IfPermitted>
          </Alert>
        )
        : (
          <StreamList streams={streams}
                      streamRuleTypes={streamRuleTypes}
                      permissions={currentUser.permissions}
                      user={currentUser}
                      onStreamSave={onStreamSave}
                      indexSets={indexSets} />
        )}
      </div>
    </PaginatedList>
  );
};

StreamComponent.propTypes = {
  onStreamSave: PropTypes.func.isRequired,
  indexSets: PropTypes.array.isRequired,
};

export default StreamComponent;
