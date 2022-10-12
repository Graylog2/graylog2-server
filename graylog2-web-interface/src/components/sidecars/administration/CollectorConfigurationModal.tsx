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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import { Table, BootstrapModalWrapper, Button, Modal } from 'components/bootstrap';
import { SearchForm, Icon } from 'components/common';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import { Link } from 'components/common/router';

import type { Collector, Configuration, SidecarSummary } from '../types';

const ConfigurationContainer = styled.div`
  overflow: auto;
  height: 360px;
  margin-top: 8px;
`;

const ConfigurationTable = styled(Table)`
  margin-bottom: 0;
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

const SecondaryText = styled.div`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #aaaaaa;
  margin-top: -4px;
  margin-bottom: -2px;
`;

const TableRow = styled.tr(({ disabled = false }: {disabled?: boolean}) => css`
  cursor: ${disabled ? 'auto' : 'pointer'};
  background-color: ${disabled ? '#E8E8E8 !important' : 'initial'};
  border-bottom: 1px solid lightgray;
  height: 49px;
`);

const StickyTableRowFooter = styled.tr`
  height: 32px;
  position: sticky;
  bottom: -1px;
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

type Props = {
  show: boolean,
  onCancel: () => void,
  onSave: (selectedConfigurations: string[], partiallySelectedConfigurations: string[]) => void,
  selectedCollectorName: string,
  selectedSidecarNames: string[],
  initialAssignedConfigs: string[],
  initialPartiallyAssignedConfigs: string[],
  unassignedConfigs: string[],
  getRowData: (configName: string) => {
    configuration: Configuration,
    collector: Collector,
    sidecars: SidecarSummary[],
    autoAssignedTags: string[],
  }
};

const CollectorConfigurationModal = ({
  show,
  onCancel,
  onSave,
  selectedCollectorName,
  selectedSidecarNames,
  initialAssignedConfigs,
  initialPartiallyAssignedConfigs,
  unassignedConfigs,
  getRowData,
}: Props) => {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedConfigurations, setSelectedConfigurations] = useState<string[]>(initialAssignedConfigs);
  const [partiallySelectedConfigurations, setPartiallySelectedConfigurations] = useState<string[]>(initialPartiallyAssignedConfigs);

  const onReset = () => {
    setSelectedConfigurations(initialAssignedConfigs);
    setPartiallySelectedConfigurations(initialPartiallyAssignedConfigs);
    setSearchQuery('');
  };

  const isNotDirty = lodash.isEqual(selectedConfigurations, initialAssignedConfigs) && lodash.isEqual(partiallySelectedConfigurations, initialPartiallyAssignedConfigs);

  const searchQueryRegexPattern = new RegExp(searchQuery, 'i');

  const filteredOptions = [...initialAssignedConfigs, ...initialPartiallyAssignedConfigs, ...unassignedConfigs].filter((configuration) => configuration.match(searchQueryRegexPattern));

  const rows = filteredOptions.map((configName) => {
    const { configuration, collector, sidecars, autoAssignedTags } = getRowData(configName);

    const selected = selectedConfigurations.includes(configName);
    const partiallySelected = !selected && partiallySelectedConfigurations.includes(configName);
    const secondaryText = (selected && selectedSidecarNames.join(', ')) || (partiallySelected && sidecars.map((sidecar) => sidecar.node_name).join(', ')) || '';
    const isAssignedFromTags = autoAssignedTags.length > 0;

    return (
      <TableRow key={configName}
                disabled={isAssignedFromTags}
                onClick={() => {
                  if (!isAssignedFromTags) {
                    if (partiallySelected) {
                      setPartiallySelectedConfigurations(partiallySelectedConfigurations.filter((name) => name !== configName));
                    } else {
                      setSelectedConfigurations(selected ? selectedConfigurations.filter((name) => name !== configName) : [...selectedConfigurations, configName]);
                    }
                  }
                }}>
        <IconTableCell>
          {selected && <Icon name="check" title={`${configName} is selected`} />}
          {partiallySelected && <Icon type="regular" name="square-minus" title={`${configName} is selected`} />}
        </IconTableCell>
        <IconTableCell><ColorLabel color={configuration.color} size="xsmall" /></IconTableCell>
        <ConfigurationTableCell>
          {configName}
          <SecondaryText title={secondaryText}>
            <small>{secondaryText}</small>
          </SecondaryText>
        </ConfigurationTableCell>
        <IconTableCell>
          {isAssignedFromTags && <Icon name="lock" title={`Assigned from tags: ${autoAssignedTags.join(', ')}`} />}
        </IconTableCell>
        <CollectorTableCell>
          <small>
            {collector
              ? <CollectorIndicator collector={collector.name} operatingSystem={collector.node_operating_system} />
              : <em>Unknown collector</em>}
          </small>
        </CollectorTableCell>
        <UnselectTableCell>{(selected || partiallySelected) && !isAssignedFromTags && <Icon name="times" title={`Remove ${configName}`} />}</UnselectTableCell>
      </TableRow>
    );
  });

  return (
    <BootstrapModalWrapper showModal={show} onHide={onCancel}>
      <Modal.Header>
        <ModalTitle>
          Edit <b>{selectedCollectorName}</b> Configurations
          <ModalSubTitle>
            <small>
              {`${selectedSidecarNames.length} sidecar${selectedSidecarNames.length > 1 ? 's' : ''}: `}
              {selectedSidecarNames.join(', ')}
            </small>
          </ModalSubTitle>
        </ModalTitle>
      </Modal.Header>
      <Modal.Body>
        <SearchForm query={searchQuery} onQueryChange={(q) => setSearchQuery(q)} topMargin={0} queryWidth="100%" />
        <ConfigurationContainer>
          <ConfigurationTable className="table-condensed table-hover">
            <tbody>
              {(rows.length === 0) ? (
                <TableRow>
                  <td colSpan={6}>
                    <NoConfigurationMessage>No configurations available for the selected log collector.</NoConfigurationMessage>
                  </td>
                </TableRow>
              ) : (
                rows
              )}
              <StickyTableRowFooter>
                <td colSpan={6}>
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
        <Button type="button" onClick={onCancel}>Cancel</Button>
        <Button type="button" onClick={onReset}>Reset</Button>
        <Button type="submit" bsStyle="primary" disabled={isNotDirty} onClick={() => onSave(selectedConfigurations, partiallySelectedConfigurations)}>Save</Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

CollectorConfigurationModal.propTypes = {
  show: PropTypes.bool.isRequired,
  selectedCollectorName: PropTypes.string.isRequired,
  selectedSidecarNames: PropTypes.array.isRequired,
  initialAssignedConfigs: PropTypes.array.isRequired,
  initialPartiallyAssignedConfigs: PropTypes.array.isRequired,
  unassignedConfigs: PropTypes.array.isRequired,
  onCancel: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  getRowData: PropTypes.func.isRequired,
};

export default CollectorConfigurationModal;
