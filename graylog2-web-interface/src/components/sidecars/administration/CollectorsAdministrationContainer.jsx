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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import lodash from 'lodash';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { CollectorConfigurationsActions, CollectorConfigurationsStore } from 'stores/sidecars/CollectorConfigurationsStore';
import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';
import { SidecarsActions } from 'stores/sidecars/SidecarsStore';
import { SidecarsAdministrationActions, SidecarsAdministrationStore } from 'stores/sidecars/SidecarsAdministrationStore';

import CollectorsAdministration, { PAGE_SIZES } from './CollectorsAdministration';

const CollectorsAdministrationContainer = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    nodeId: PropTypes.string,
    paginationQueryParameter: PropTypes.object.isRequired,
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

  handlePageChange(page, pageSize) {
    const { filters, query } = this.state.sidecars;

    SidecarsAdministrationActions.list({ query, filters, page, pageSize });
  },

  handleFilter(property, value) {
    const { resetPage, pageSize } = this.props.paginationQueryParameter;
    const { filters, query } = this.state.sidecars;
    let newFilters;

    if (property) {
      newFilters = lodash.cloneDeep(filters);
      newFilters[property] = value;
    } else {
      newFilters = {};
    }

    resetPage();

    SidecarsAdministrationActions.list({ query, filters: newFilters, pageSize, page: 1 });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { resetPage, pageSize } = this.props.paginationQueryParameter;
    const { filters } = this.state.sidecars;

    resetPage();

    SidecarsAdministrationActions.list({ query, filters, pageSize, page: 1 }).finally(callback);
  },

  handleConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback) {
    SidecarsActions.assignConfigurations(selectedSidecars, selectedConfigurations).then((response) => {
      doneCallback();
      const { query, filters } = this.state.sidecars;
      const { page, pageSize } = this.props.paginationQueryParameter;

      SidecarsAdministrationActions.list({ query, filters, pageSize, page });

      return response;
    });
  },

  handleProcessAction(action, selectedCollectors, doneCallback) {
    SidecarsAdministrationActions.setAction(action, selectedCollectors).then((response) => {
      doneCallback();

      return response;
    });
  },

  reloadSidecars() {
    if (this.state.sidecars) {
      SidecarsAdministrationActions.refreshList();
    }
  },

  loadData(nodeId) {
    const { page, pageSize } = this.props.paginationQueryParameter;
    const query = nodeId ? `node_id:${nodeId}` : '';

    CollectorsActions.all();
    SidecarsAdministrationActions.list({ query, page, pageSize });
    CollectorConfigurationsActions.all();
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

export default withPaginationQueryParameter(CollectorsAdministrationContainer, { pageSizes: PAGE_SIZES });
