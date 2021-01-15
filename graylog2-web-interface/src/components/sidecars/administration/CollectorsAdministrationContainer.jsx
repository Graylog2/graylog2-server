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
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import lodash from 'lodash';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import CombinedProvider from 'injection/CombinedProvider';
import { Spinner } from 'components/common';

import CollectorsAdministration from './CollectorsAdministration';

const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');
const { SidecarsAdministrationStore, SidecarsAdministrationActions } = CombinedProvider.get('SidecarsAdministration');
const { CollectorConfigurationsStore, CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');
const { SidecarsActions } = CombinedProvider.get('Sidecars');

const CollectorsAdministrationContainer = createReactClass({
  propTypes: {
    nodeId: PropTypes.string,
  },

  mixins: [Reflux.connect(CollectorsStore, 'collectors'), Reflux.connect(SidecarsAdministrationStore, 'sidecars'), Reflux.connect(CollectorConfigurationsStore, 'configurations')],

  getDefaultProps() {
    return {
      nodeId: undefined,
    };
  },

  componentDidMount() {
    this.loadData(this.props.nodeId);
    this.interval = setInterval(this.reloadSidecars, 5000);
  },

  componentDidUpdate(prevProps) {
    if (prevProps.nodeId !== this.props.nodeId) {
      // This means the user changed the URL, so we don't need to keep the previous state.
      this.loadData(this.props.nodeId);
    }
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  loadData(nodeId) {
    const query = nodeId ? `node_id:${nodeId}` : '';

    CollectorsActions.all();
    SidecarsAdministrationActions.list({ query: query });
    CollectorConfigurationsActions.all();
  },

  reloadSidecars() {
    if (this.state.sidecars) {
      SidecarsAdministrationActions.refreshList();
    }
  },

  handlePageChange(page, pageSize) {
    const { filters, pagination, query } = this.state.sidecars;
    const effectivePage = pagination.pageSize !== pageSize ? 1 : page;

    SidecarsAdministrationActions.list({ query: query, filters: filters, page: effectivePage, pageSize: pageSize });
  },

  handleFilter(property, value) {
    const { filters, pagination, query } = this.state.sidecars;
    let newFilters;

    if (property) {
      newFilters = lodash.cloneDeep(filters);
      newFilters[property] = value;
    } else {
      newFilters = {};
    }

    SidecarsAdministrationActions.list({ query: query, filters: newFilters, pageSize: pagination.pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { filters, pagination } = this.state.sidecars;

    SidecarsAdministrationActions.list({ query: query, filters: filters, pageSize: pagination.pageSize }).finally(callback);
  },

  handleConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback) {
    SidecarsActions.assignConfigurations(selectedSidecars, selectedConfigurations).then((response) => {
      doneCallback();
      const { query, filters, pagination } = this.state.sidecars;

      SidecarsAdministrationActions.list({ query: query, filters: filters, pageSize: pagination.pageSize, page: pagination.page });

      return response;
    });
  },

  handleProcessAction(action, selectedCollectors, doneCallback) {
    SidecarsAdministrationActions.setAction(action, selectedCollectors).then((response) => {
      doneCallback();

      return response;
    });
  },

  render() {
    const { collectors, configurations, sidecars } = this.state;

    if (!collectors || !collectors.collectors || !sidecars || !sidecars.sidecars || !configurations || !configurations.configurations) {
      return <Spinner text="Loading collector list..." />;
    }

    const sidecarCollectors = [];

    sidecars.sidecars
      .sort((s1, s2) => naturalSortIgnoreCase(s1.node_name, s2.node_name))
      .forEach((sidecar) => {
        const compatibleCollectorIds = sidecar.collectors;

        if (lodash.isEmpty(compatibleCollectorIds)) {
          sidecarCollectors.push({ collector: {}, sidecar: sidecar });

          return;
        }

        compatibleCollectorIds
          .map((id) => lodash.find(collectors.collectors, { id: id }))
          .forEach((compatibleCollector) => {
            sidecarCollectors.push({ collector: compatibleCollector, sidecar: sidecar });
          });
      });

    return (
      <CollectorsAdministration sidecarCollectorPairs={sidecarCollectors}
                                collectors={collectors.collectors}
                                configurations={configurations.configurations}
                                pagination={sidecars.pagination}
                                query={sidecars.query}
                                filters={sidecars.filters}
                                onPageChange={this.handlePageChange}
                                onFilter={this.handleFilter}
                                onQueryChange={this.handleQueryChange}
                                onConfigurationChange={this.handleConfigurationChange}
                                onProcessAction={this.handleProcessAction} />
    );
  },
});

export default CollectorsAdministrationContainer;
