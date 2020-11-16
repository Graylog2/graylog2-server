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
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

import SidecarList from './SidecarList';

const { SidecarsStore, SidecarsActions } = CombinedProvider.get('Sidecars');

const SidecarListContainer = createReactClass({
  displayName: 'SidecarListContainer',
  mixins: [Reflux.connect(SidecarsStore)],

  componentDidMount() {
    this._reloadSidecars({});
    this.interval = setInterval(() => this._reloadSidecars({}), this.SIDECAR_DATA_REFRESH);
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  SIDECAR_DATA_REFRESH: 5 * 1000,

  _reloadSidecars({ query, page, pageSize, onlyActive, sortField, order }) {
    const effectiveQuery = query === undefined ? this.state.query : query;

    const options = {
      query: effectiveQuery,
      onlyActive: 'true',
    };

    if (this.state.sort) {
      options.sortField = sortField || this.state.sort.field;
      options.order = order || this.state.sort.order;
    }

    if (this.state.pagination) {
      options.pageSize = pageSize || this.state.pagination.pageSize;
      options.onlyActive = onlyActive === undefined ? this.state.onlyActive : onlyActive; // Avoid || to handle false values
      const shouldKeepPage = options.pageSize === this.state.pagination.pageSize
        && options.onlyActive === this.state.onlyActive
        && options.query === this.state.query; // Only keep page number when other parameters don't change
      let effectivePage = 1;

      if (shouldKeepPage) {
        effectivePage = page || this.state.pagination.page;
      }

      options.page = effectivePage;
    }

    return SidecarsActions.listPaginated(options);
  },

  toggleShowInactive() {
    this._reloadSidecars({ onlyActive: !this.state.onlyActive });
  },

  handleSortChange(field) {
    return () => {
      this._reloadSidecars({
        sortField: field,
        order: (this.state.sort.field === field ? (this.state.sort.order === 'asc' ? 'desc' : 'asc') : 'asc'),
      });
    };
  },

  handlePageChange(page, pageSize) {
    this._reloadSidecars({ page: page, pageSize: pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    this._reloadSidecars({ query: query }).finally(callback);
  },

  render() {
    const { sidecars, onlyActive, pagination, query, sort } = this.state;

    const isLoading = !sidecars;

    if (isLoading) {
      return <Spinner />;
    }

    return (
      <SidecarList sidecars={sidecars}
                   onlyActive={onlyActive}
                   pagination={pagination}
                   query={query}
                   sort={sort}
                   onPageChange={this.handlePageChange}
                   onQueryChange={this.handleQueryChange}
                   onSortChange={this.handleSortChange}
                   toggleShowInactive={this.toggleShowInactive} />
    );
  },
});

export default SidecarListContainer;
