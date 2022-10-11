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
import React, { useState, useRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled from 'styled-components';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { BootstrapModalConfirm } from 'components/bootstrap';

import CollectorConfigurationModal from './CollectorConfigurationModal';

const ConfigurationSummary = styled.div`
  word-break: break-all;
`;

const CollectorConfigurationModalContainer = ({
  collectors,
  configurations,
  selectedSidecarCollectorPairs,
  onConfigurationSelectionChange,
  show,
  onCancel,
}) => {
  const [nextAssignedConfigurations, setNextAssignedConfigurations] = useState([]);
  const [nextPartiallyAssignedConfigurations, setNextPartiallyAssignedConfigurations] = useState([]);
  const modalConfirm = useRef(null);

  const getSelectedLogCollector = () => {
    return (lodash.uniq<any>(selectedSidecarCollectorPairs.map(({ collector }) => collector)))[0];
  };

  const sortConfigurationNames = (configs) => {
    return configs.sort((config1, config2) => naturalSortIgnoreCase(config1.name, config2.name))
      .map((config) => config.name);
  };

  const getAssignedConfigurations = (_selectedSidecarCollectorPairs, selectedCollector) => {
    const assignments = _selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar).reduce((accumulator, sidecar) => accumulator.concat(sidecar.assignments), []);

    const filteredAssignments = assignments.map((assignment) => configurations.find((configuration) => configuration.id === assignment.configuration_id))
      .filter((configuration) => selectedCollector?.id === configuration.collector_id);

    return sortConfigurationNames(filteredAssignments);
  };

  const getUnassignedConfigurations = (assignedConfigurations: string[], selectedCollector) => {
    const filteredConfigs = configurations.filter((config) => !assignedConfigurations.includes(config.name) && (selectedCollector?.id === config.collector_id));

    return sortConfigurationNames(filteredConfigs);
  };

  const getFullyAndPartiallyAssignments = (_assignedConfigurations: string[]) => {
    const occurrences = lodash.countBy(_assignedConfigurations);

    return [
      lodash.uniq<any>(_assignedConfigurations.filter((config) => occurrences[config] === selectedSidecarCollectorPairs.length)),
      lodash.uniq<any>(_assignedConfigurations.filter((config) => occurrences[config] < selectedSidecarCollectorPairs.length)),
    ];
  };

  const onSave = (fullyAssignedConfigs: string[], partiallyAssignedConfigs: string[]) => {
    setNextAssignedConfigurations(fullyAssignedConfigs);
    setNextPartiallyAssignedConfigurations(partiallyAssignedConfigs);
    modalConfirm.current.open();
  };

  const confirmConfigurationChange = (doneCallback: () => void) => {
    const assignedConfigurationsToSave = configurations.filter((config) => nextAssignedConfigurations.includes(config.name));

    selectedSidecarCollectorPairs.forEach((sidecarCollectorPair) => {
      let configs = assignedConfigurationsToSave;

      if (nextPartiallyAssignedConfigurations.length) {
        const selectedLogCollector = getSelectedLogCollector();
        const assignments = getAssignedConfigurations([sidecarCollectorPair], selectedLogCollector);
        const assignmentsToKeep = lodash.intersection(assignments, nextPartiallyAssignedConfigurations);
        const assignedConfigurationsToKeep = configurations.filter((config) => assignmentsToKeep.includes(config.name));
        configs = [...assignedConfigurationsToSave, ...assignedConfigurationsToKeep];
      }

      onConfigurationSelectionChange([sidecarCollectorPair], configs, doneCallback);
    });

    onCancel();
  };

  const cancelConfigurationChange = () => {
    setNextAssignedConfigurations([]);
  };

  const getConfiguration = (configName: string) => {
    return configurations.find((config) => config.name === configName);
  };

  const getCollector = (configName: string) => {
    const configuration = getConfiguration(configName);

    return collectors.find((collector) => collector.id === configuration.collector_id);
  };

  const getSidecars = (configName: string) => {
    const configuration = getConfiguration(configName);

    return selectedSidecarCollectorPairs.filter(({ sidecar }) => sidecar.assignments.map((assignment) => assignment.configuration_id).includes(configuration.id)).map((assignment) => assignment.sidecar);
  };

  const getAssignedFromTags = (configId: string, collectorId: string, sidecars) => {
    const assigned_from_tags = sidecars.reduce((accumulator, sidecar) => {
      return accumulator.concat(
        sidecar.assignments.find((assignment) => (assignment.collector_id === collectorId) && (assignment.configuration_id === configId)).assigned_from_tags,
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
    const sidecarsSummary = selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name).join(', ');
    const numberOfSidecarsSummary = `${selectedSidecarCollectorPairs.length} sidecars`;
    const summary = selectedSidecarCollectorPairs.length <= 5 ? sidecarsSummary : numberOfSidecarsSummary;

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

  const MemoizedConfigurationModal = useMemo(() => {
    const renderConfigurationModal = () => {
      const selectedCollector = getSelectedLogCollector();
      const assignedConfigurations = getAssignedConfigurations(selectedSidecarCollectorPairs, selectedCollector);
      const unassignedConfigurations = getUnassignedConfigurations(assignedConfigurations, selectedCollector);
      const [initialAssignedConfigs, initialPartiallyAssignedConfigs] = getFullyAndPartiallyAssignments(assignedConfigurations);

      return (
        <CollectorConfigurationModal show={show}
                                     selectedCollectorName={selectedCollector?.name || ''}
                                     selectedSidecarNames={selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name)}
                                     initialAssignedConfigs={initialAssignedConfigs}
                                     initialPartiallyAssignedConfigs={initialPartiallyAssignedConfigs}
                                     unassignedConfigs={unassignedConfigurations}
                                     onCancel={onCancel}
                                     onSave={onSave}
                                     getRowData={getRowData} />
      );
    };

    return renderConfigurationModal;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [show]);

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
