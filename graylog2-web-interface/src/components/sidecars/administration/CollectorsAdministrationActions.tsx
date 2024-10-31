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
import React, { useState, useCallback } from 'react';
import uniq from 'lodash/uniq';
import styled from 'styled-components';

import { ButtonToolbar, Button } from 'components/bootstrap';
import { Icon } from 'components/common';

import CollectorConfigurationModalContainer from './CollectorConfigurationModalContainer';
import CollectorProcessControl from './CollectorProcessControl';

import type { Collector, Configuration, SidecarCollectorPairType } from '../types';

const ConfigurationButton = styled(Button)`
  margin-right: 6px;
`;

type Props = {
  collectors: Collector[],
  configurations: Configuration[],
  selectedSidecarCollectorPairs: SidecarCollectorPairType[],
  onConfigurationSelectionChange: (pairs: SidecarCollectorPairType[], configs: Configuration[], callback: () => void) => void,
  onProcessAction: (action: string, pairs: SidecarCollectorPairType[], callback: () => void) => void,
};

const CollectorsAdministrationActions = ({
  collectors,
  configurations,
  selectedSidecarCollectorPairs,
  onConfigurationSelectionChange,
  onProcessAction,
}: Props) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const onCancelConfigurationModal = useCallback(() => setShowConfigurationModal(false), []);

  const selectedLogCollectorsNames = uniq(selectedSidecarCollectorPairs.map(({ collector }) => collector.name));
  const disableConfigButton = !selectedSidecarCollectorPairs.length || selectedLogCollectorsNames.length !== 1;

  const getConfigButtonTooltip = () => {
    if (!selectedSidecarCollectorPairs.length) {
      return 'Incompatible collectors, please check your sidecar configuration.';
    }

    if (selectedLogCollectorsNames.length !== 1) {
      return `Cannot change configurations of ${selectedLogCollectorsNames.join(', ')} collectors simultaneously.`;
    }

    return 'Assign Configurations';
  };

  return (
    <ButtonToolbar>
      <ConfigurationButton title={getConfigButtonTooltip()}
                           bsStyle="primary"
                           bsSize="small"
                           disabled={disableConfigButton}
                           onClick={() => setShowConfigurationModal(true)}>
        <Icon name="edit_square" /> Assign Configurations
      </ConfigurationButton>
      <CollectorConfigurationModalContainer collectors={collectors}
                                            configurations={configurations}
                                            selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                            onConfigurationSelectionChange={onConfigurationSelectionChange}
                                            show={showConfigurationModal}
                                            onCancel={onCancelConfigurationModal} />
      <CollectorProcessControl selectedSidecarCollectorPairs={selectedSidecarCollectorPairs} onProcessAction={onProcessAction} />
    </ButtonToolbar>
  );
};

export default CollectorsAdministrationActions;
