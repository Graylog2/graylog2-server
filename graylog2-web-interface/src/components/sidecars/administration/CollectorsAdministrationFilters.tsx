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
import find from 'lodash/find';
import uniq from 'lodash/uniq';
import upperFirst from 'lodash/upperFirst';

import { SelectPopover } from 'components/common';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import type { Collector } from 'components/sidecars/types';

type Configuration = {
  id: string,
  name: string,
  color: string,
}
type Props = {
  collectors: Collector[],
  configurations: Configuration[],
  filters: {
    collector?: string,
    configuration?: string,
    os?: string,
    status?: string,
  },
  filter: (name?: string, value?: string) => void,
}

const CollectorsAdministrationFilters = (props: Props) => {
  const onFilterChange = (name: string, value: string, callback: () => void) => {
    const { filter } = props;

    filter(name, value);
    callback();
  };

  const getCollectorsFilter = () => {
    const { collectors, filters } = props;
    const collectorMapper = (collector: Collector) => `${collector.id};${collector.name}`;

    const collectorItems = collectors
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      // TODO: Hack to be able to filter in SelectPopover. We should change that to avoid this hack.
      .map(collectorMapper);

    const collectorFormatter = (collectorId: string) => {
      const [id] = collectorId.split(';');
      const collector = find(collectors, { id: id });

      return <CollectorIndicator collector={collector.name} operatingSystem={collector.node_operating_system} />;
    };

    const filter = ([collectorId]: Array<string>, callback: () => void) => {
      const [id] = collectorId ? collectorId.split(';') : [];

      onFilterChange('collector', id, callback);
    };

    let collectorFilter;

    if (filters.collector) {
      const collector = collectors.find((c) => c.id === filters.collector);

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
  };

  const getConfigurationFilter = () => {
    const { configurations, filters } = props;

    const configurationMapper = (configuration: Configuration) => `${configuration.id};${configuration.name}`;
    const configurationItems = configurations
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      // TODO: Hack to be able to filter in SelectPopover. We should change that to avoid this hack.
      .map(configurationMapper);

    const configurationFormatter = (configurationId: string) => {
      const [id] = configurationId.split(';');
      const configuration = find(configurations, { id: id });

      return <span><ColorLabel color={configuration.color} size="xsmall" /> {configuration.name}</span>;
    };

    const filter = ([configurationId]: Array<string>, callback: () => void) => {
      const [id] = configurationId ? configurationId.split(';') : [];

      onFilterChange('configuration', id, callback);
    };

    let configurationFilter;

    if (filters.configuration) {
      const configuration = configurations.find((c) => c.id === filters.configuration);

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
  };

  const getOSFilter = () => {
    const { collectors, filters } = props;

    const operatingSystems = uniq(collectors.map((collector) => upperFirst(collector.node_operating_system)))
      .sort(naturalSortIgnoreCase);

    const filter = ([os]: Array<string>, callback: () => void) => onFilterChange('os', os, callback);

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
  };

  const getStatusFilter = () => {
    const { filters } = props;
    const status = Object.keys(SidecarStatusEnum.properties).map((key) => String(key));
    const filter = ([statusCode]: Array<string>, callback: () => void) => onFilterChange('status', statusCode, callback);

    const statusFilter = filters.status;
    const statusFormatter = (statusCode: string) => upperFirst(SidecarStatusEnum.toString(statusCode));

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
  };

  return (
    <ButtonToolbar>
      {getCollectorsFilter()}
      {getConfigurationFilter()}
      {getStatusFilter()}
      {getOSFilter()}
    </ButtonToolbar>
  );
};

export default CollectorsAdministrationFilters;
