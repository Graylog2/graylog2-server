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
import React, { useState, useMemo } from 'react';
import countBy from 'lodash/countBy';
import intersection from 'lodash/intersection';
import uniq from 'lodash/uniq';
import styled from 'styled-components';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { BootstrapModalConfirm } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import CollectorConfigurationModal from './CollectorConfigurationModal';

import type { Collector, Configuration, SidecarCollectorPairType, SidecarSummary } from '../types';

const ConfigurationSummary = styled.div`
  word-break: break-all;
`;

type Props = {
  collectors: Collector[],
  configurations: Configuration[],
  selectedSidecarCollectorPairs: SidecarCollectorPairType[],
  onConfigurationSelectionChange: (pairs: SidecarCollectorPairType[], configs: Configuration[], callback: () => void) => void,
  show: boolean,
  onCancel: () => void,
};

const CollectorConfigurationModalContainer = ({
  collectors,
  configurations,
  selectedSidecarCollectorPairs,
  onConfigurationSelectionChange,
  show,
  onCancel,
}: Props) => {
  const [nextAssignedConfigurations, setNextAssignedConfigurations] = useState<string[]>([]);
  const [nextPartiallyAssignedConfigurations, setNextPartiallyAssignedConfigurations] = useState<string[]>([]);
  const [showConfirmModal, setShowConfirmModal] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry();

  const getSelectedLogCollector = () => (uniq<Collector>(selectedSidecarCollectorPairs.map(({ collector }) => collector)))[0];

  const sortConfigurationNames = (configs: Configuration[]) => configs.sort((config1, config2) => naturalSortIgnoreCase(config1.name, config2.name))
    .map((config) => config.name);

  const getAssignedConfigurations = (_selectedSidecarCollectorPairs: SidecarCollectorPairType[], selectedCollector: Collector) => {
    const assignments = _selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar).reduce((accumulator, sidecar) => accumulator.concat(sidecar.assignments), []);

    const filteredAssignments = assignments.map((assignment) => configurations.find((configuration) => configuration.id === assignment.configuration_id))
      .filter((configuration) => selectedCollector?.id === configuration.collector_id);

    return sortConfigurationNames(filteredAssignments);
  };

  const getUnassignedConfigurations = (assignedConfigurations: string[], selectedCollector: Collector) => {
    const filteredConfigs = configurations.filter((config) => !assignedConfigurations.includes(config.name) && (selectedCollector?.id === config.collector_id));

    return sortConfigurationNames(filteredConfigs);
  };

  const getFullyAndPartiallyAssignments = (_assignedConfigurations: string[]) => {
    const occurrences = countBy(_assignedConfigurations);

    return [
      uniq<string>(_assignedConfigurations.filter((config) => occurrences[config] === selectedSidecarCollectorPairs.length)),
      uniq<string>(_assignedConfigurations.filter((config) => occurrences[config] < selectedSidecarCollectorPairs.length)),
    ];
  };

  const onSave = (fullyAssignedConfigs: string[], partiallyAssignedConfigs: string[]) => {
    setNextAssignedConfigurations(fullyAssignedConfigs);
    setNextPartiallyAssignedConfigurations(partiallyAssignedConfigs);
    setShowConfirmModal(true);
  };

  const cancelConfigurationChange = () => {
    setNextAssignedConfigurations([]);
    setNextPartiallyAssignedConfigurations([]);
    setShowConfirmModal(false);
  };

  const confirmConfigurationChange = () => {
    const assignedConfigurationsToSave = configurations.filter((config) => nextAssignedConfigurations.includes(config.name));

    selectedSidecarCollectorPairs.forEach((sidecarCollectorPair) => {
      let configs = assignedConfigurationsToSave;

      if (nextPartiallyAssignedConfigurations.length) {
        const selectedLogCollector = getSelectedLogCollector();
        const assignments = getAssignedConfigurations([sidecarCollectorPair], selectedLogCollector);
        const assignmentsToKeep = intersection(assignments, nextPartiallyAssignedConfigurations);
        const assignedConfigurationsToKeep = configurations.filter((config) => assignmentsToKeep.includes(config.name));
        configs = [...assignedConfigurationsToSave, ...assignedConfigurationsToKeep];
      }

      sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.CONFIGURATION_ASSIGNED, {
        app_pathname: 'sidecars',
        app_section: 'administration',
      });

      onConfigurationSelectionChange([sidecarCollectorPair], configs, () => {});
    });

    cancelConfigurationChange();
    onCancel();
  };

  const getConfiguration = (configName: string) => configurations.find((config) => config.name === configName);

  const getCollector = (configName: string) => {
    const configuration = getConfiguration(configName);

    return collectors.find((collector) => collector.id === configuration.collector_id);
  };

  const getSidecars = (configName: string) => {
    const configuration = getConfiguration(configName);

    return selectedSidecarCollectorPairs.filter(({ sidecar }) => sidecar.assignments.map((assignment) => assignment.configuration_id).includes(configuration.id)).map((assignment) => assignment.sidecar);
  };

  const getAssignedFromTags = (configId: string, collectorId: string, sidecars: SidecarSummary[]) => {
    const assigned_from_tags = sidecars.reduce((accumulator, sidecar) => accumulator.concat(
      sidecar.assignments.find((assignment) => (assignment.collector_id === collectorId) && (assignment.configuration_id === configId)).assigned_from_tags,
    ), []);

    return uniq(assigned_from_tags);
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
      <BootstrapModalConfirm showModal={showConfirmModal}
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

export default CollectorConfigurationModalContainer;
