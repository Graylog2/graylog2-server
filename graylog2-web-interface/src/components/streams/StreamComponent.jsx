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

import { Alert } from 'components/graylog';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import Spinner from 'components/common/Spinner';

import StreamList from './StreamList';
import CreateStreamButton from './CreateStreamButton';

const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

class StreamComponent extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    onStreamSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      pagination: {
        page: 1,
        perPage: 10,
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

  loadData = (callback) => {
    const { state } = this;
    const { page, perPage, query } = state.pagination;

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
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });

    this.setState({ pagination: newPagination }, this.loadData);
  };

  _onSearch = (query, resetLoadingCallback) => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: query });

    this.setState({ pagination: newPagination }, () => this.loadData(resetLoadingCallback));
  };

  _onReset = () => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: '' });

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
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState />
          </div>
          <div>{streamListComp}</div>
        </PaginatedList>
      </div>
    );
  }
}

export default StreamComponent;
