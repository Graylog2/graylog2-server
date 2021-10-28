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
import PropTypes from 'prop-types';
import React from 'react';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { DeflectorActions } from 'stores/indices/DeflectorStore';
import { IndexRangesActions } from 'stores/indices/IndexRangesStore';

class IndicesMaintenanceDropdown extends React.Component {
  static propTypes = {
    indexSetId: PropTypes.string.isRequired,
    indexSet: PropTypes.object,
  };

  _onRecalculateIndexRange = () => {
    if (window.confirm('This will recalculate index ranges for this index set using a background system job. Do you want to proceed?')) {
      const { indexSetId } = this.props;
      IndexRangesActions.recalculate(indexSetId);
    }
  };

  _onCycleDeflector = () => {
    if (window.confirm('This will manually cycle the current active write index on this index set. Do you want to proceed?')) {
      const { indexSetId } = this.props;

      DeflectorActions.cycle(indexSetId).then(() => {
        DeflectorActions.list(indexSetId);
      });
    }
  };

  render() {
    let cycleButton;

    const { indexSet } = this.props;

    if (indexSet?.writable) {
      cycleButton = <MenuItem eventKey="2" onClick={this._onCycleDeflector}>Rotate active write index</MenuItem>;
    }

    return (
      <ButtonGroup>
        <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
          <MenuItem eventKey="1" onClick={this._onRecalculateIndexRange}>Recalculate index ranges</MenuItem>
          {cycleButton}
        </DropdownButton>
      </ButtonGroup>
    );
  }
}

export default IndicesMaintenanceDropdown;
