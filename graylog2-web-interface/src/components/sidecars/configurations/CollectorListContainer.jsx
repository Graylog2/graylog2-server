import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import CollectorList from './CollectorList';

import CollectorsStore from './CollectorsStore';
import CollectorsActions from './CollectorsActions';

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
    const query = this.state.collectors.query;
    CollectorsActions.list({ query: query, page: page, pageSize: pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const pageSize = this.state.collectors.pagination.pageSize;
    CollectorsActions.list({ query: query, pageSize: pageSize }).finally(callback);
  },

  validateCollector(name) {
    return CollectorsActions.validate(name);
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
                     onPageChange={this.handlePageChange}
                     onQueryChange={this.handleQueryChange}
                     onClone={this.handleClone}
                     onDelete={this.handleDelete}
                     validateCollector={this.validateCollector} />
    );
  },
});

export default CollectorListContainer;
