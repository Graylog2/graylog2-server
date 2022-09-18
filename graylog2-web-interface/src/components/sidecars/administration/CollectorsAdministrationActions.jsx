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

import { ButtonToolbar, Button } from 'components/bootstrap';
import { Icon } from 'components/common';

import CollectorConfigurationModal from './CollectorConfigurationModal';
import CollectorProcessControl from './CollectorProcessControl';

const ConfigurationButton = styled(Button)`
  margin-right: 6px
`;

const CollectorsAdministrationActions = (props) => {
  const [showConfigurationModal, setShowConfigurationModal] = React.useState(false);
  const onCancelConfigurationModal = React.useCallback(() => setShowConfigurationModal(false), []);

  const { collectors, configurations, selectedSidecarCollectorPairs, onConfigurationSelectionChange, onProcessAction } = props;
  const selectedLogCollectorsNames = lodash.uniq(selectedSidecarCollectorPairs.map(({ collector }) => collector.name));

  return (
    <ButtonToolbar>
      <ConfigurationButton title={(selectedLogCollectorsNames.length > 1) ? `Cannot change configurations of ${selectedLogCollectorsNames.join(', ')} collectors simultaneously` : undefined}
                           bsStyle="primary"
                           bsSize="small"
                           disabled={selectedLogCollectorsNames.length !== 1}
                           onClick={() => setShowConfigurationModal(true)}>
        <Icon name="edit" /> Edit Configurations
      </ConfigurationButton>
      <CollectorConfigurationModal collectors={collectors}
                                   configurations={configurations}
                                   selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                   onConfigurationSelectionChange={onConfigurationSelectionChange}
                                   show={showConfigurationModal}
                                   onCancel={onCancelConfigurationModal} />
      <CollectorProcessControl selectedSidecarCollectorPairs={selectedSidecarCollectorPairs} onProcessAction={onProcessAction} />
    </ButtonToolbar>
  );
};

CollectorsAdministrationActions.propTypes = {
  collectors: PropTypes.array.isRequired,
  configurations: PropTypes.array.isRequired,
  selectedSidecarCollectorPairs: PropTypes.array.isRequired,
  onConfigurationSelectionChange: PropTypes.func.isRequired,
  onProcessAction: PropTypes.func.isRequired,
};

export default CollectorsAdministrationActions;
