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

import ConfigurationList from './ConfigurationList';

const { CollectorConfigurationsStore, CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');
const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');

const ConfigurationListContainer = createReactClass({
  mixins: [Reflux.connect(CollectorConfigurationsStore, 'configurations'), Reflux.connect(CollectorsStore, 'collectors')],

  componentDidMount() {
    this._reloadConfiguration();
  },

  _reloadConfiguration() {
    CollectorConfigurationsActions.list({});
    CollectorsActions.all();
  },

  validateConfiguration(configuration) {
    return CollectorConfigurationsActions.validate(configuration);
  },

  handlePageChange(page, pageSize) {
    const { query } = this.state.configurations;

    CollectorConfigurationsActions.list({ query: query, page: page, pageSize: pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { pageSize } = this.state.configurations.pagination;

    CollectorConfigurationsActions.list({ query: query, pageSize: pageSize }).finally(callback);
  },

  handleClone(configuration, name, callback) {
    CollectorConfigurationsActions.copyConfiguration(configuration, name)
      .then((response) => {
        callback();

        return response;
      });
  },

  handleDelete(configuration) {
    CollectorConfigurationsActions.delete(configuration);
  },

  render() {
    const { collectors, configurations } = this.state;
    const isLoading = !collectors || !collectors.collectors || !configurations || !configurations.paginatedConfigurations;

    if (isLoading) {
      return <Spinner />;
    }

    return (
      <ConfigurationList collectors={collectors.collectors}
                         query={configurations.query}
                         pagination={configurations.pagination}
                         total={configurations.total}
                         configurations={configurations.paginatedConfigurations}
                         onPageChange={this.handlePageChange}
                         onQueryChange={this.handleQueryChange}
                         onClone={this.handleClone}
                         onDelete={this.handleDelete}
                         validateConfiguration={this.validateConfiguration} />
    );
  },
});

export default ConfigurationListContainer;
