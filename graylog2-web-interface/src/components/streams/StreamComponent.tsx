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
import React from 'react';

import { Alert } from 'components/bootstrap';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';

import StreamList from './StreamList';
import CreateStreamButton from './CreateStreamButton';

class StreamComponent extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    onStreamSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
    paginationQueryParameter: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      pagination: {
        count: 0,
        total: 0,
        query: '',
      },
    };
  }

  componentDidMount() {
    this.loadData();

    StreamRulesStore.types().then((types) => {
      this.setState({ streamRuleTypes: types });
    });

    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  }

  componentWillUnmount() {
    StreamsStore.unregister(this.loadData);
    StreamRulesStore.unregister(this.loadData);
  }

  loadData = (callback, page = this.props.paginationQueryParameter.page, perPage = this.props.paginationQueryParameter.pageSize) => {
    const { query } = this.state.pagination;

    StreamsStore.searchPaginated(page, perPage, query)
      .then(({ streams, pagination }) => {
        this.setState({
          streams: streams,
          pagination: pagination,
        });
      })
      .then(() => {
        if (callback) {
          callback();
        }
      });
  };

  _isLoading = () => {
    const { state } = this;

    return !(state.streams && state.streamRuleTypes);
  };

  _onPageChange = (newPage, newPerPage) => {
    this.loadData(null, newPage, newPerPage);
  };

  _onSearch = (query, resetLoadingCallback) => {
    const { pagination } = this.state;
    const newPagination = { ...pagination, query: query };
    this.props.paginationQueryParameter.resetPage();
    this.setState({ pagination: newPagination }, () => this.loadData(resetLoadingCallback));
  };

  _onReset = () => {
    const { pagination } = this.state;
    const newPagination = { ...pagination, query: '' };
    this.props.paginationQueryParameter.resetPage();
    this.setState({ pagination: newPagination }, this.loadData);
  };

  render() {
    const { streams, pagination, streamRuleTypes } = this.state;
    const { currentUser, onStreamSave, indexSets } = this.props;

    if (this._isLoading()) {
      return (
        <div style={{ marginLeft: 10 }}>
          <Spinner />
        </div>
      );
    }

    const createStreamButton = (
      <IfPermitted permissions="streams:create">
        <CreateStreamButton bsSize="small"
                            bsStyle="link"
                            className="btn-text"
                            buttonText="Create one now"
                            indexSets={indexSets}
                            onSave={onStreamSave} />
      </IfPermitted>
    );

    const noStreams = (
      <Alert bsStyle="warning">
        <Icon name="info-circle" />&nbsp;No streams found. {createStreamButton}
      </Alert>
    );

    const streamsList = (
      <StreamList streams={streams}
                  streamRuleTypes={streamRuleTypes}
                  permissions={currentUser.permissions}
                  user={currentUser}
                  onStreamSave={onStreamSave}
                  indexSets={indexSets} />
    );

    const streamListComp = streams.length === 0
      ? noStreams
      : streamsList;

    return (
      <div>
        <PaginatedList onChange={this._onPageChange}
                       totalItems={pagination.total}>
          <div style={{ marginBottom: 15 }}>
            <SearchForm onSearch={this._onSearch}
                        onReset={this._onReset}
                        queryHelpComponent={<QueryHelper entityName="stream" />}
                        useLoadingState />
          </div>
          <div>{streamListComp}</div>
        </PaginatedList>
      </div>
    );
  }
}

export default withPaginationQueryParameter(StreamComponent);
