import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { ButtonToolbar } from 'react-bootstrap';
import { Button } from 'components/graylog';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { SelectPopover } from 'components/common';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

const CollectorsAdministrationFilters = createReactClass({
  propTypes: {
    collectors: PropTypes.array.isRequired,
    configurations: PropTypes.array.isRequired,
    filters: PropTypes.object.isRequired,
    filter: PropTypes.func.isRequired,
  },

  onFilterChange(name, value, callback) {
    const { filter } = this.props;

    filter(name, value);
    callback();
  },

  getCollectorsFilter() {
    const { collectors, filters } = this.props;
    const collectorMapper = collector => `${collector.id};${collector.name}`;

    const collectorItems = collectors
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      // TODO: Hack to be able to filter in SelectPopover. We should change that to avoid this hack.
      .map(collectorMapper);

    const collectorFormatter = (collectorId) => {
      const [id] = collectorId.split(';');
      const collector = lodash.find(collectors, { id: id });
      return <CollectorIndicator collector={collector.name} operatingSystem={collector.node_operating_system} />;
    };

    const filter = ([collectorId], callback) => {
      const [id] = collectorId ? collectorId.split(';') : [];
      this.onFilterChange('collector', id, callback);
    };

    let collectorFilter;
    if (filters.collector) {
      const collector = collectors.find(c => c.id === filters.collector);
      collectorFilter = collector ? collectorMapper(collector) : undefined;
    }

    return (
      <SelectPopover id="collector-filter"
                     title="Filter by collector"
                     triggerNode={<Button bsSize="small" bsStyle="link">Collector <span className="caret" /></Button>}
                     items={collectorItems}
                     itemFormatter={collectorFormatter}
                     onItemSelect={filter}
                     selectedItems={collectorFilter ? [collectorFilter] : []}
                     filterPlaceholder="Filter by collector" />
    );
  },

  getConfigurationFilter() {
    const { configurations, filters } = this.props;

    const configurationMapper = configuration => `${configuration.id};${configuration.name}`;
    const configurationItems = configurations
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      // TODO: Hack to be able to filter in SelectPopover. We should change that to avoid this hack.
      .map(configurationMapper);

    const configurationFormatter = (configurationId) => {
      const [id] = configurationId.split(';');
      const configuration = lodash.find(configurations, { id: id });
      return <span><ColorLabel color={configuration.color} size="xsmall" /> {configuration.name}</span>;
    };

    const filter = ([configurationId], callback) => {
      const [id] = configurationId ? configurationId.split(';') : [];
      this.onFilterChange('configuration', id, callback);
    };

    let configurationFilter;
    if (filters.configuration) {
      const configuration = configurations.find(c => c.id === filters.configuration);
      configurationFilter = configuration ? configurationMapper(configuration) : undefined;
    }

    return (
      <SelectPopover id="configuration-filter"
                     title="Filter by configuration"
                     triggerNode={<Button bsSize="small" bsStyle="link">Configuration <span className="caret" /></Button>}
                     items={configurationItems}
                     itemFormatter={configurationFormatter}
                     onItemSelect={filter}
                     selectedItems={configurationFilter ? [configurationFilter] : []}
                     filterPlaceholder="Filter by configuration" />
    );
  },

  getOSFilter() {
    const { collectors, filters } = this.props;

    const operatingSystems = lodash
      .uniq(collectors.map(collector => lodash.upperFirst(collector.node_operating_system)))
      .sort(naturalSortIgnoreCase);

    const filter = ([os], callback) => this.onFilterChange('os', os, callback);

    const osFilter = filters.os;

    return (
      <SelectPopover id="os-filter"
                     title="Filter by OS"
                     triggerNode={<Button bsSize="small" bsStyle="link">OS <span className="caret" /></Button>}
                     items={operatingSystems}
                     onItemSelect={filter}
                     selectedItems={osFilter ? [osFilter] : []}
                     filterPlaceholder="Filter by OS" />
    );
  },

  getStatusFilter() {
    const { filters } = this.props;
    const status = Object.keys(SidecarStatusEnum.properties).map(key => String(key));
    const filter = ([statusCode], callback) => this.onFilterChange('status', statusCode, callback);

    const statusFilter = filters.status;
    const statusFormatter = statusCode => lodash.upperFirst(SidecarStatusEnum.toString(statusCode));

    return (
      <SelectPopover id="status-filter"
                     title="Filter by collector status"
                     triggerNode={<Button bsSize="small" bsStyle="link">Collector Status <span className="caret" /></Button>}
                     items={status}
                     itemFormatter={statusFormatter}
                     onItemSelect={filter}
                     selectedItems={statusFilter ? [statusFilter] : []}
                     filterPlaceholder="Filter by collector status" />
    );
  },

  render() {
    return (
      <ButtonToolbar>
        {this.getCollectorsFilter()}
        {this.getConfigurationFilter()}
        {this.getStatusFilter()}
        {this.getOSFilter()}
      </ButtonToolbar>
    );
  },
});

export default CollectorsAdministrationFilters;
