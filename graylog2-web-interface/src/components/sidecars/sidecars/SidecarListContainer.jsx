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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { SidecarsActions, SidecarsStore } from 'stores/sidecars/SidecarsStore';

import SidecarList, { PAGE_SIZES } from './SidecarList';

const SidecarListContainer = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    paginationQueryParameter: PropTypes.object.isRequired,
  },

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

  handleSortChange(field) {
    return () => {
      this._reloadSidecars({
        sortField: field,
        // eslint-disable-next-line no-nested-ternary
        order: (this.state.sort.field === field ? (this.state.sort.order === 'asc' ? 'desc' : 'asc') : 'asc'),
      });
    };
  },

  handlePageChange(page, pageSize) {
    this._reloadSidecars({ page: page, pageSize: pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { resetPage } = this.props.paginationQueryParameter;

    resetPage();

    this._reloadSidecars({ query: query }).finally(callback);
  },

  toggleShowInactive() {
    const { resetPage } = this.props.paginationQueryParameter;

    resetPage();

    this._reloadSidecars({ onlyActive: !this.state.onlyActive });
  },

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

    const { paginationQueryParameter } = this.props;

    options.pageSize = pageSize || paginationQueryParameter.pageSize;
    options.onlyActive = onlyActive === undefined ? this.state.onlyActive : onlyActive; // Avoid || to handle false values

    const shouldKeepPage = options.pageSize === paginationQueryParameter.pageSize
      && options.onlyActive === this.state.onlyActive
      && options.query === this.state.query; // Only keep page number when other parameters don't change
    let effectivePage = 1;

    if (shouldKeepPage) {
      effectivePage = page || paginationQueryParameter.page;
    }

    options.page = effectivePage;

    return SidecarsActions.listPaginated(options);
  },

  SIDECAR_DATA_REFRESH: 5 * 1000,

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

export default withPaginationQueryParameter(SidecarListContainer, { pageSizes: PAGE_SIZES });
