/* eslint-disable react/prop-types */
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
import lodash from 'lodash';
import styled from 'styled-components';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { BootstrapModalConfirm } from 'components/bootstrap';

import CollectorConfigurationModal from './CollectorConfigurationModal';

const ConfigurationSummary = styled.div`
  word-break: break-all;
`;

const CollectorConfigurationModalContainer = (props) => {
  const [nextAssignedConfigurations, setNextAssignedConfigurations] = React.useState([]);
  const [nextPartiallyAssignedConfigurations, setNextPartiallyAssignedConfigurations] = React.useState([]);
  const modalConfirm = React.useRef(null);

  const getSelectedLogCollector = () => {
    return (lodash.uniq<any>(props.selectedSidecarCollectorPairs.map(({ collector }) => collector)))[0];
  };

  const getAssignedConfigurations = (_selectedSidecarCollectorPairs, selectedCollector) => {
    const assignments = _selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar).reduce((accumulator, sidecar) => accumulator.concat(sidecar.assignments), []);

    return assignments.map((assignment) => props.configurations.find((configuration) => configuration.id === assignment.configuration_id))
      .filter((configuration) => selectedCollector?.id === configuration.collector_id)
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      .map((config) => config.name);
  };

  const getUnassignedConfigurations = (assignedConfigurations: string[], selectedCollector) => {
    return props.configurations.filter((c) => !assignedConfigurations.includes(c.name) && (selectedCollector?.id === c.collector_id))
      .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
      .map((c) => c.name);
  };

  const getFullyAndPartiallyAssignments = (_assignedConfigurations: string[]) => {
    const occurrences = lodash.countBy(_assignedConfigurations);

    return [
      lodash.uniq<any>(_assignedConfigurations.filter((a) => occurrences[a] === props.selectedSidecarCollectorPairs.length)),
      lodash.uniq<any>(_assignedConfigurations.filter((a) => occurrences[a] < props.selectedSidecarCollectorPairs.length)),
    ];
  };

  const onSave = (fullyAssignedConfigs: string[], partiallyAssignedConfigs: string[]) => {
    setNextAssignedConfigurations(fullyAssignedConfigs);
    setNextPartiallyAssignedConfigurations(partiallyAssignedConfigs);
    modalConfirm.current.open();
  };

  const confirmConfigurationChange = (doneCallback: () => void) => {
    const assignedConfigurationsToSave = props.configurations.filter((c) => nextAssignedConfigurations.includes(c.name));

    props.selectedSidecarCollectorPairs.forEach((sidecarCollectorPair) => {
      let configs = assignedConfigurationsToSave;

      if (nextPartiallyAssignedConfigurations.length) {
        const selectedLogCollector = getSelectedLogCollector();
        const assignments = getAssignedConfigurations([sidecarCollectorPair], selectedLogCollector);
        const assignmentsToKeep = lodash.intersection(assignments, nextPartiallyAssignedConfigurations);
        const assignedConfigurationsToKeep = props.configurations.filter((c) => assignmentsToKeep.includes(c.name));
        configs = [...assignedConfigurationsToSave, ...assignedConfigurationsToKeep];
      }

      props.onConfigurationSelectionChange([sidecarCollectorPair], configs, doneCallback);
    });

    props.onCancel();
  };

  const cancelConfigurationChange = () => {
    setNextAssignedConfigurations([]);
  };

  const getConfiguration = (configName: string) => {
    return props.configurations.find((c) => c.name === configName);
  };

  const getCollector = (configName: string) => {
    const configuration = getConfiguration(configName);

    return props.collectors.find((b) => b.id === configuration.collector_id);
  };

  const getSidecars = (configName: string) => {
    const configuration = getConfiguration(configName);

    return props.selectedSidecarCollectorPairs.filter(({ sidecar }) => sidecar.assignments.map((s) => s.configuration_id).includes(configuration.id)).map((a) => a.sidecar);
  };

  const getAssignedFromTags = (configId: string, collectorId: string, sidecars) => {
    const assigned_from_tags = sidecars.reduce((accumulator, sidecar) => {
      return accumulator.concat(
        sidecar.assignments.find((a) => (a.collector_id === collectorId) && (a.configuration_id === configId)).assigned_from_tags,
      );
    }, []);

    return lodash.uniq(assigned_from_tags);
  };

  const getRowData = (configName: string) => {
    const configuration = getConfiguration(configName);
    const collector = getCollector(configName);
    const sidecars = getSidecars(configName);
    const autoAssignedTags = getAssignedFromTags(configuration.id, collector.id, sidecars);

    return { configuration, collector, sidecars, autoAssignedTags };
  };

  const renderConfigurationSummary = () => {
    const sidecarsSummary = props.selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name).join(', ');
    const numberOfSidecarsSummary = `${props.selectedSidecarCollectorPairs.length} sidecars`;
    const summary = props.selectedSidecarCollectorPairs.length <= 5 ? sidecarsSummary : numberOfSidecarsSummary;

    return (
      <BootstrapModalConfirm ref={modalConfirm}
                             title="Configuration summary"
                             onConfirm={confirmConfigurationChange}
                             onCancel={cancelConfigurationChange}>
        <ConfigurationSummary>
          <p>Are you sure you want to proceed with this action for <b>{summary}</b>?</p>
        </ConfigurationSummary>
      </BootstrapModalConfirm>
    );
  };

  const MemoizedConfigurationModal = React.useMemo(() => {
    const renderConfigurationModal = () => {
      const selectedCollector = getSelectedLogCollector();
      const assignedConfigurations = getAssignedConfigurations(props.selectedSidecarCollectorPairs, selectedCollector);
      const unassignedConfigurations = getUnassignedConfigurations(assignedConfigurations, selectedCollector);
      const [initialAssignedConfigs, initialPartiallyAssignedConfigs] = getFullyAndPartiallyAssignments(assignedConfigurations);

      return (
        <CollectorConfigurationModal show={props.show}
                                     selectedCollectorName={selectedCollector?.name || ''}
                                     selectedSidecarNames={props.selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name)}
                                     initialAssignedConfigs={initialAssignedConfigs}
                                     initialPartiallyAssignedConfigs={initialPartiallyAssignedConfigs}
                                     unassignedConfigs={unassignedConfigurations}
                                     onCancel={props.onCancel}
                                     onSave={onSave}
                                     getRowData={getRowData} />
      );
    };

    return renderConfigurationModal;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.show]);

  return (
    <>
      <MemoizedConfigurationModal />
      {renderConfigurationSummary()}
    </>
  );
};

CollectorConfigurationModalContainer.propTypes = {
  collectors: PropTypes.array.isRequired,
  configurations: PropTypes.array.isRequired,
  selectedSidecarCollectorPairs: PropTypes.array.isRequired,
  onConfigurationSelectionChange: PropTypes.func.isRequired,
  show: PropTypes.bool.isRequired,
  onCancel: PropTypes.func.isRequired,
};

export default CollectorConfigurationModalContainer;
