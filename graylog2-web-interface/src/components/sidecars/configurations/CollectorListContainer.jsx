import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';
import { Spinner } from 'components/common';

import CollectorList from './CollectorList';

const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');

const CollectorListContainer = createReactClass({
  mixins: [Reflux.connect(CollectorsStore, 'collectors')],

  componentDidMount() {
    this.loadCollectors();
  },

  loadCollectors() {
    CollectorsActions.list({});
  },

  handleClone(collector, name, callback) {
    CollectorsActions.copy(collector, name)
      .then(() => {
        callback();
      });
  },

  handleDelete(collector) {
    CollectorsActions.delete(collector);
  },

  handlePageChange(page, pageSize) {
    const { query } = this.state.collectors;
    CollectorsActions.list({ query: query, page: page, pageSize: pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { pageSize } = this.state.collectors.pagination;
    CollectorsActions.list({ query: query, pageSize: pageSize }).finally(callback);
  },

  validateCollector(collector) {
    return CollectorsActions.validate(collector);
  },

  render() {
    const { collectors } = this.state;
    if (!collectors || !collectors.paginatedCollectors) {
      return <Spinner />;
    }

    return (
      <CollectorList collectors={collectors.paginatedCollectors}
                     pagination={collectors.pagination}
                     query={collectors.query}
                     total={collectors.total}
                     onPageChange={this.handlePageChange}
                     onQueryChange={this.handleQueryChange}
                     onClone={this.handleClone}
                     onDelete={this.handleDelete}
                     validateCollector={this.validateCollector} />
    );
  },
});

export default CollectorListContainer;
