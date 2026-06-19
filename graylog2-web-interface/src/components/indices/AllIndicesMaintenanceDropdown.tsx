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
import * as React from 'react';
import { useState } from 'react';

import { SystemIndexRanges } from '@graylog/server-api';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import OutdatedIndicesModal from 'components/indices/OutdatedIndicesModal';

const _onRecalculateAllIndexRange = () => {
  // eslint-disable-next-line no-alert
  if (window.confirm('This will cleanup & recalculate all index ranges existing. Do you want to proceed?')) {
    SystemIndexRanges.rebuild();
  }
};

const AllIndicesMaintenanceDropdown = () => {
  const [showOutdatedModal, setShowOutdatedModal] = useState(false);

  return (
    <ButtonGroup>
      <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
        <MenuItem eventKey="1" onClick={_onRecalculateAllIndexRange}>
          Cleanup & recalculate all index ranges
        </MenuItem>
        <MenuItem eventKey="4" onClick={() => setShowOutdatedModal(true)}>
          Show outdated indices
        </MenuItem>
      </DropdownButton>
      {showOutdatedModal && <OutdatedIndicesModal show onClose={() => setShowOutdatedModal(false)} />}
    </ButtonGroup>
  );
};

export default AllIndicesMaintenanceDropdown;
