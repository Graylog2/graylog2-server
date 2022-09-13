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
import { naturalSortIgnoreCase } from 'util/SortUtils';

import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled from 'styled-components';

import { Table, BootstrapModalConfirm, BootstrapModalWrapper, Button, Modal } from 'components/bootstrap';
import { SearchForm, Icon } from 'components/common';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';

const ConfigurationContainer = styled.div`
  overflow: auto;
  height: 300px;
  margin-top: 8px
`;

const ConfigurationTable = styled(Table)`
  margin-bottom: 0
`;

const ConfigurationButton = styled(Button)`
  margin-right: 6px
`;

const TableRow = styled.tr`
  cursor: pointer;
  border-bottom: 1px solid lightgray;
  height: 32px;
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

const CollectorConfigurationSelector = (props) => {
  const [nextAssignedConfigurations, setNextAssignedConfigurations] = React.useState([]);
  const [show, setShow] = React.useState(false);

  const modalConfirm = React.useRef(null);
  const onCancel = React.useCallback(() => setShow(false), []);

  const getAssignedConfigurations = (selectedSidecarCollectorPairs, configurations) => {
    const assignments = selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar).reduce((accumulator, sidecar) => accumulator.concat(sidecar.assignments), []);

    return assignments.map((assignment) => configurations.find((configuration) => configuration.id === assignment.configuration_id));
  };

  const onSave = (configurationNames: string[]) => {
    setNextAssignedConfigurations(configurationNames);
    modalConfirm.current.open();
  };

  const confirmConfigurationChange = (doneCallback) => {
    const { onConfigurationSelectionChange, configurations } = props;
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

  const renderConfigurationSummary = (_previousAssignedConfigurations, _nextAssignedConfigurations, _selectedSidecarCollectorPairs) => {
    const toAdd = lodash.difference(_nextAssignedConfigurations, _previousAssignedConfigurations);
    const toRemove = lodash.difference(_previousAssignedConfigurations, _nextAssignedConfigurations);
    console.log(toAdd, toRemove);
    const exampleSidecarCollectorPair = _selectedSidecarCollectorPairs[0];
    const collectorIndicator = (
      <em>
        <CollectorIndicator collector={exampleSidecarCollectorPair.collector.name}
                            operatingSystem={exampleSidecarCollectorPair.collector.node_operating_system} />
      </em>
    );

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
        <div>
          {toAddSummary}
          {toRemoveSummary}
          <p>Are you sure you want to proceed with this action for <b>{formattedSummary}</b>?</p>
        </div>
      </BootstrapModalConfirm>
    );
  };

  const { configurations, selectedSidecarCollectorPairs } = props;

  // Do not allow configuration changes when more than one log collector type is selected
  const selectedLogCollectors = lodash.uniq(selectedSidecarCollectorPairs.map(({ collector }) => collector)) as any[];

  const assignedConfigurations = getAssignedConfigurations(selectedSidecarCollectorPairs, configurations)
    .filter((configuration) => selectedLogCollectors[0].id === configuration.collector_id)
    .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
    .map((config) => config.name);

  const nonAssignedConfigurations = configurations.filter((c) => !assignedConfigurations.includes(c.name));

  const MemoizedModalForm = React.useMemo(() => {
    // eslint-disable-next-line react/no-unstable-nested-components
    const ModalForm = () => {
      const [searchQuery, setSearchQuery] = React.useState<string>('');
      const [selectedConfigurations, setSelectedConfigurations] = React.useState<string[]>(assignedConfigurations);

      const isSelected = (name: string) => selectedConfigurations.includes(name);

      const options = nonAssignedConfigurations
        .filter((configuration) => (selectedLogCollectors[0].id === configuration.collector_id))
        .sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name))
        .map((c) => c.name);

      const filteredOptions = [...assignedConfigurations, ...options].filter((configuration) => configuration.match(new RegExp(searchQuery, 'i')));

      const rows = filteredOptions.map((option) => {
        const selected = isSelected(option);
        const config = getConfiguration(option);
        const collector = getCollector(option);

        return (
          <TableRow key={option}
                    onClick={() => setSelectedConfigurations(selected ? selectedConfigurations.filter((c) => c !== option) : [...selectedConfigurations, option])}>
            <IconTableCell>{selected && <Icon name="check" title={`${option} is selected`} />}</IconTableCell>
            <IconTableCell><ColorLabel color={config.color} size="xsmall" /></IconTableCell>
            <ConfigurationTableCell>{option}</ConfigurationTableCell>
            <CollectorTableCell>
              <small>
                {collector
                  ? <CollectorIndicator collector={collector.name} operatingSystem={collector.node_operating_system} />
                  : <em>Unknown collector</em>}
              </small>
            </CollectorTableCell>
            <UnselectTableCell>{selected && <Icon name="times" title={`Remove ${option}`} />}</UnselectTableCell>
          </TableRow>
        );
      });

      return (
        <BootstrapModalWrapper showModal={show}>
          <Modal.Header>
            <Modal.Title>Configure</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <SearchForm onQueryChange={(q) => setSearchQuery(q)} topMargin={0} queryWidth="100%" />
            <ConfigurationContainer>
              <ConfigurationTable className="table-condensed table-hover">
                <tbody>
                  {rows}
                </tbody>
              </ConfigurationTable>
            </ConfigurationContainer>
          </Modal.Body>
          <Modal.Footer>
            <Button type="button" onClick={onCancel}>Cancel</Button>
            <Button type="submit" bsStyle="primary" onClick={() => onSave(selectedConfigurations)}>Save</Button>
          </Modal.Footer>
        </BootstrapModalWrapper>
      );
    };

    return <ModalForm />;
  }, [show, onCancel]);

  return (
    <>
      <ConfigurationButton bsStyle="primary" bsSize="small" onClick={() => setShow(true)}>Configure</ConfigurationButton>
      {MemoizedModalForm}
      {renderConfigurationSummary(assignedConfigurations, nextAssignedConfigurations, selectedSidecarCollectorPairs)}
    </>
  );

  // if (selectedLogCollectors.length > 1) {
  //   return (
  //     <SelectPopover id="status-filter"
  //                    title="Apply configuration"
  //                    triggerNode={<Button bsSize="small" bsStyle="link">Configure <span className="caret" /></Button>}
  //                    items={[`Cannot change configurations of ${selectedLogCollectors.map((collector) => collector.name).join(', ')} collectors simultaneously`]}
  //                    displayDataFilter={false}
  //                    disabled />
  //   );
  // }

  // if (configurationIds.length === 0) {
  //   return (
  //     <SelectPopover id="status-filter"
  //                    title="Apply configuration"
  //                    triggerNode={<Button bsSize="small" bsStyle="link">Configure <span className="caret" /></Button>}
  //                    items={['No configurations available for the selected log collector']}
  //                    displayDataFilter={false}
  //                    disabled />
  //   );
  // }
};

CollectorConfigurationSelector.propTypes = {
  collectors: PropTypes.array.isRequired,
  configurations: PropTypes.array.isRequired,
  selectedSidecarCollectorPairs: PropTypes.array.isRequired,
  onConfigurationSelectionChange: PropTypes.func.isRequired,
};

export default CollectorConfigurationSelector;
