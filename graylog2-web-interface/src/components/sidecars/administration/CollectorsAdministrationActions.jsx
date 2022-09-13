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

import { ButtonToolbar } from 'components/bootstrap';

import CollectorConfigurationModal from './CollectorConfigurationModal';
import CollectorProcessControl from './CollectorProcessControl';

const CollectorsAdministrationActions = (props) => {
  const { collectors, configurations, selectedSidecarCollectorPairs, onConfigurationSelectionChange, onProcessAction } = props;

  return (
    <ButtonToolbar>
      <CollectorConfigurationModal collectors={collectors}
                                   configurations={configurations}
                                   selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                   onConfigurationSelectionChange={onConfigurationSelectionChange} />
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
