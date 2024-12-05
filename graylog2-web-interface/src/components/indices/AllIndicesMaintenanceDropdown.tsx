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

import { SystemIndexRanges } from '@graylog/server-api';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';

const _onRecalculateAllIndexRange = () => {
  // eslint-disable-next-line no-alert
  if (window.confirm('This will cleanup & recalculate all index ranges existing. Do you want to proceed?')) {
    SystemIndexRanges.rebuild();
  }
};

const AllIndicesMaintenanceDropdown = () => (
  <ButtonGroup>
    <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
      <MenuItem eventKey="1" onClick={_onRecalculateAllIndexRange}>Cleanup & recalculate all index ranges</MenuItem>
    </DropdownButton>
  </ButtonGroup>
);

export default AllIndicesMaintenanceDropdown;
