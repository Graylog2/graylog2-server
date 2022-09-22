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
import Routes from 'routing/Routes';
import { Table, BootstrapModalConfirm, BootstrapModalWrapper, Button, Modal } from 'components/bootstrap';
import { SearchForm, Icon } from 'components/common';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import { Link } from 'components/common/router';

const ConfigurationContainer = styled.div`
  overflow: auto;
  height: 360px;
  margin-top: 8px
`;

const ConfigurationTable = styled(Table)`
  margin-bottom: 0
`;

const NoConfigurationMessage = styled.div`
  display: flex;
  justify-content: center;
`;

const AddNewConfiguration = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
`;

const ConfigurationSummary = styled.div`
  word-break: break-all;
`;

const SecondaryText = styled.div`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #aaaaaa;
  margin-top: -6px;
  margin-bottom: -2px;
`;

const TableRow = styled.tr`
  cursor: pointer;
  border-bottom: 1px solid lightgray;
  height: 49px;
`;

const StickyTableRowFooter = styled.tr`
  height: 32px;
  position: sticky;
  bottom: 0;
`;

const IconTableCell = styled.td`
  width: 32px;
`;

const CollectorTableCell = styled.td`
  width: 140px;
  text-align: right;
`;

const ConfigurationTableCell = styled.td`
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 0;
`;

const UnselectTableCell = styled.td`
  width: 32px;
  text-align: center;
`;

const ModalTitle = styled(Modal.Title)`
  font-size: 1.266rem !important;
  line-height: 1.1;
`;

const ModalSubTitle = styled.div`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
`;

const CollectorConfigurationModal = (props) => {
  const [nextAssignedConfigurations, setNextAssignedConfigurations] = React.useState([]);

  const modalConfirm = React.useRef(null);

  const getAssignedConfigurations = () => {
    const { selectedSidecarCollectorPairs, configurations } = props;
    const assignments = selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar).reduce((accumulator, sidecar) => accumulator.concat(sidecar.assignments), []);

    return assignments.map((assignment) => configurations.find((configuration) => configuration.id === assignment.configuration_id));
  };

  const getFullyAndPartiallyAssignments = (_assignedConfigurations) => {
    const { selectedSidecarCollectorPairs } = props;
    const occurrences = lodash.countBy(_assignedConfigurations);

    return [
      lodash.uniq(_assignedConfigurations.filter((a) => occurrences[a] === selectedSidecarCollectorPairs.length)) as any[],
      lodash.uniq(_assignedConfigurations.filter((a) => occurrences[a] < selectedSidecarCollectorPairs.length)) as any[],
    ];
  };

  const onSave = (configurationNames: string[]) => {
    setNextAssignedConfigurations(configurationNames);
    modalConfirm.current.open();
  };

  const confirmConfigurationChange = (doneCallback) => {
    const { onConfigurationSelectionChange, configurations, onCancel } = props;
    const assignedConfigurationsToSave = configurations.filter((c) => nextAssignedConfigurations.includes(c.name));

    onConfigurationSelectionChange(assignedConfigurationsToSave, doneCallback);
    onCancel();
  };

  const cancelConfigurationChange = () => {
    setNextAssignedConfigurations([]);
  };

  const getConfiguration = (name: string) => {
    return props.configurations.find((c) => c.name === name);
  };

  const getCollector = (name: string) => {
    const configuration = getConfiguration(name);

    return props.collectors.find((b) => b.id === configuration.collector_id);
  };

  const getSidecars = (name: string) => {
    const configuration = getConfiguration(name);

    return props.selectedSidecarCollectorPairs.filter(({ sidecar }) => sidecar.assignments.map((s) => s.configuration_id).includes(configuration.id)).map((a) => a.sidecar.node_name);
  };

  const renderConfigurationSummary = (_previousAssignedConfigurations, _nextAssignedConfigurations, _selectedSidecarCollectorPairs) => {
    const toAdd = lodash.difference(_nextAssignedConfigurations, _previousAssignedConfigurations);
    const toRemove = lodash.difference(_previousAssignedConfigurations, _nextAssignedConfigurations);
    const exampleSidecarCollectorPair = _selectedSidecarCollectorPairs[0];
    const collectorIndicator = exampleSidecarCollectorPair ? (
      <em>
        <CollectorIndicator collector={exampleSidecarCollectorPair.collector.name}
                            operatingSystem={exampleSidecarCollectorPair.collector.node_operating_system} />
      </em>
    ) : null;

    let toAddSummary;
    let toRemoveSummary;

    if (toRemove.length > 0) {
      toAddSummary = <p><span>You are going to <b>remove</b> the configuration <b>{toRemove.join(', ')}</b> for collector {collectorIndicator}</span></p>;
    }

    if (toAdd.length > 0) {
      toRemoveSummary = <p><span>You are going to <b>apply</b> the configuration <b>{toAdd.join(', ')}</b> for collector {collectorIndicator}</span></p>;
    }

    const formattedSummary = _selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name).join(', ');

    return (
      <BootstrapModalConfirm ref={modalConfirm}
                             title="Configuration summary"
                             onConfirm={confirmConfigurationChange}
                             onCancel={cancelConfigurationChange}>
        <ConfigurationSummary>
          {toAddSummary}
          {toRemoveSummary}
          <p>Are you sure you want to proceed with this action for <b>{formattedSummary}</b>?</p>
        </ConfigurationSummary>
      </BootstrapModalConfirm>
    );
  };

  const { configurations, selectedSidecarCollectorPairs } = props;

  // Do not allow configuration changes when more than one log collector type is selected
  const selectedLogCollectors = lodash.uniq(selectedSidecarCollectorPairs.map(({ collector }) => collector)) as any[];

  const assignedConfigurations = getAssignedConfigurations()
    .filter((configuration) => selectedLogCollectors[0]?.id === configuration.collector_id)
    .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
    .map((config) => config.name);

  const [fullyAssignedConfigurations, partiallyAssignedConfigurations] = getFullyAndPartiallyAssignments(assignedConfigurations);

  const nonAssignedConfigurations = configurations.filter((c) => !assignedConfigurations.includes(c.name));

  const MemoizedModalForm = React.useMemo(() => {
    // eslint-disable-next-line react/no-unstable-nested-components
    const ModalForm = () => {
      const [searchQuery, setSearchQuery] = React.useState<string>('');
      const [selectedConfigurations, setSelectedConfigurations] = React.useState<string[]>(fullyAssignedConfigurations);
      const [partiallySelectedConfigurations, setPartiallySelectedConfigurations] = React.useState<string[]>(partiallyAssignedConfigurations);

      const onReset = () => {
        setSelectedConfigurations(fullyAssignedConfigurations);
        setPartiallySelectedConfigurations(partiallyAssignedConfigurations);
      };

      const isNotDirty = lodash.isEqual(selectedConfigurations, fullyAssignedConfigurations) && lodash.isEqual(partiallySelectedConfigurations, partiallyAssignedConfigurations);

      const allSidecarNames = selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name);

      const options = nonAssignedConfigurations
        .filter((configuration) => (selectedLogCollectors[0]?.id === configuration.collector_id))
        .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
        .map((c) => c.name);

      const filteredOptions = [...fullyAssignedConfigurations, ...partiallyAssignedConfigurations, ...options].filter((configuration) => configuration.match(new RegExp(searchQuery, 'i')));

      const rows = filteredOptions.map((configName) => {
        const config = getConfiguration(configName);
        const collector = getCollector(configName);
        const sidecars = getSidecars(configName);
        const selected = selectedConfigurations.includes(configName);
        const partiallySelected = !selected && partiallySelectedConfigurations.includes(configName);
        const secondaryText = (selected && allSidecarNames.join(', ')) || (partiallySelected && sidecars.join(', ')) || '';

        return (
          <TableRow key={configName}
                    onClick={() => {
                      if (partiallySelected) {
                        setPartiallySelectedConfigurations(partiallySelectedConfigurations.filter((c) => c !== configName));
                      } else {
                        setSelectedConfigurations(selected ? selectedConfigurations.filter((c) => c !== configName) : [...selectedConfigurations, configName]);
                      }
                    }}>
            <IconTableCell>
              {selected && <Icon name="check" title={`${configName} is selected`} />}
              {partiallySelected && <Icon type="regular" name="square-minus" title={`${configName} is selected`} />}
            </IconTableCell>
            <IconTableCell><ColorLabel color={config.color} size="xsmall" /></IconTableCell>
            <ConfigurationTableCell>
              {configName}
              <SecondaryText title={secondaryText}>
                <small>{secondaryText}</small>
              </SecondaryText>
            </ConfigurationTableCell>
            <CollectorTableCell>
              <small>
                {collector
                  ? <CollectorIndicator collector={collector.name} operatingSystem={collector.node_operating_system} />
                  : <em>Unknown collector</em>}
              </small>
            </CollectorTableCell>
            <UnselectTableCell>{(selected || partiallySelected) && <Icon name="times" title={`Remove ${configName}`} />}</UnselectTableCell>
          </TableRow>
        );
      });

      return (
        <BootstrapModalWrapper showModal={props.show}>
          <Modal.Header>
            <ModalTitle>
              Edit <b>{selectedLogCollectors[0]?.name}</b> Configurations
              <ModalSubTitle>
                <small>
                  {`${allSidecarNames.length} sidecar${allSidecarNames.length > 1 ? 's' : ''}: `}
                  {allSidecarNames.join(', ')}
                </small>
              </ModalSubTitle>
            </ModalTitle>
          </Modal.Header>
          <Modal.Body>
            <SearchForm onQueryChange={(q) => setSearchQuery(q)} topMargin={0} queryWidth="100%" />
            <ConfigurationContainer>
              <ConfigurationTable className="table-condensed table-hover">
                <tbody>
                  {(rows.length === 0) ? (
                    <TableRow>
                      <td colSpan={5}>
                        <NoConfigurationMessage>No configurations available for the selected log collector.</NoConfigurationMessage>
                      </td>
                    </TableRow>
                  ) : (
                    rows
                  )}
                  <StickyTableRowFooter>
                    <td colSpan={5}>
                      <AddNewConfiguration>
                        <Link to={Routes.SYSTEM.SIDECARS.NEW_CONFIGURATION}><Icon name="add" />&nbsp;Add a new configuration</Link>
                      </AddNewConfiguration>
                    </td>
                  </StickyTableRowFooter>
                </tbody>
              </ConfigurationTable>
            </ConfigurationContainer>
          </Modal.Body>
          <Modal.Footer>
            <Button type="button" onClick={props.onCancel}>Cancel</Button>
            <Button type="button" onClick={onReset}>Reset</Button>
            <Button type="submit" bsStyle="primary" disabled={isNotDirty} onClick={() => onSave(selectedConfigurations)}>Save</Button>
          </Modal.Footer>
        </BootstrapModalWrapper>
      );
    };

    return <ModalForm />;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.show]);

  return (
    <>
      {MemoizedModalForm}
      {renderConfigurationSummary(assignedConfigurations, nextAssignedConfigurations, selectedSidecarCollectorPairs)}
    </>
  );
};

CollectorConfigurationModal.propTypes = {
  collectors: PropTypes.array.isRequired,
  configurations: PropTypes.array.isRequired,
  selectedSidecarCollectorPairs: PropTypes.array.isRequired,
  onConfigurationSelectionChange: PropTypes.func.isRequired,
  show: PropTypes.bool.isRequired,
  onCancel: PropTypes.func.isRequired,
};

export default CollectorConfigurationModal;
