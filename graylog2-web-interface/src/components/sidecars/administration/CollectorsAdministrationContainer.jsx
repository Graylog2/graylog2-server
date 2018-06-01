import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import lodash from 'lodash';
import { naturalSortIgnoreCase } from 'util/SortUtils';

import { Spinner } from 'components/common';
import CollectorsAdministration from './CollectorsAdministration';

import CollectorsActions from 'components/sidecars/configurations/CollectorsActions';
import CollectorsStore from 'components/sidecars/configurations/CollectorsStore';
import AdministrationActions from './AdministrationActions';
import AdministrationStore from './AdministrationStore';
import CollectorConfigurationsActions from 'components/sidecars/configurations/CollectorConfigurationsActions';
import CollectorConfigurationsStore from 'components/sidecars/configurations/CollectorConfigurationsStore';
import SidecarsActions from 'components/sidecars/sidecars/SidecarsActions';

const CollectorsAdministrationContainer = createReactClass({
  propTypes: {
    nodeId: PropTypes.string,
  },

  mixins: [Reflux.connect(CollectorsStore, 'collectors'), Reflux.connect(AdministrationStore, 'sidecars'), Reflux.connect(CollectorConfigurationsStore, 'configurations')],

  getDefaultProps() {
    return {
      nodeId: undefined,
    };
  },

  componentDidMount() {
    this.loadData(this.props.nodeId);
  },

  componentDidUpdate(prevProps) {
    if (prevProps.nodeId !== this.props.nodeId) {
      // This means the user changed the URL, so we don't need to keep the previous state.
      this.loadData(this.props.nodeId);
    }
  },

  loadData(nodeId) {
    const query = nodeId ? `node_id:${nodeId}` : '';

    CollectorsActions.all();
    AdministrationActions.list({ query: query });
    CollectorConfigurationsActions.all();
  },

  handlePageChange(page, pageSize) {
    const { filters, pagination, query } = this.state.sidecars;
    const effectivePage = pagination.pageSize !== pageSize ? 1 : page;
    AdministrationActions.list({ query: query, filters: filters, page: effectivePage, pageSize: pageSize });
  },

  handleFilter(property, value) {
    const { filters, pagination, query } = this.state.sidecars;
    const newFilters = lodash.cloneDeep(filters);
    newFilters[property] = value;
    AdministrationActions.list({ query: query, filters: newFilters, pageSize: pagination.pageSize });
  },

  handleQueryChange(query = '', callback = () => {}) {
    const { filters, pagination } = this.state.sidecars;
    AdministrationActions.list({ query: query, filters: filters, pageSize: pagination.pageSize }).finally(callback);
  },

  handleConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback) {
    SidecarsActions.assignConfigurations(selectedSidecars, selectedConfigurations).then((response) => {
      doneCallback();
      const { query, filters, pagination } = this.state.sidecars;
      AdministrationActions.list({ query: query, filters: filters, pageSize: pagination.pageSize, page: pagination.page });
      return response;
    });
  },

  handleProcessAction(action, selectedCollectors, doneCallback) {
    AdministrationActions.setAction(action, selectedCollectors).then((response) => {
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
          .map(id => lodash.find(collectors.collectors, { id: id }))
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
                                onPageChange={this.handlePageChange}
                                onFilter={this.handleFilter}
                                onQueryChange={this.handleQueryChange}
                                onConfigurationChange={this.handleConfigurationChange}
                                onProcessAction={this.handleProcessAction} />
    );
  },
});

export default CollectorsAdministrationContainer;
