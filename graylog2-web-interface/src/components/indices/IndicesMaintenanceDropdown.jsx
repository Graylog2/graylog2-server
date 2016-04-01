import React from 'react';
import { ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';

import ActionsProvider from 'injection/ActionsProvider';
const DeflectorActions = ActionsProvider.getActions('Deflector');
const IndexRangesActions = ActionsProvider.getActions('IndexRanges');

import StoreProvider from 'injection/StoreProvider';
const DeflectorStore = StoreProvider.getStore('Deflector'); // eslint-disable-line no-unused-vars

const IndicesMaintenanceDropdown = React.createClass({
  _onRecalculateIndexRange() {
    if (window.confirm('This will trigger a background system job. Go on?')) {
      IndexRangesActions.recalculate();
    }
  },
  _onCycleDeflector() {
    if (window.confirm('Really manually cycle deflector? Follow the documentation link on this page to learn more.')) {
      DeflectorActions.cycle();
    }
  },
  render() {
    return (
      <ButtonGroup>
        <DropdownButton bsStyle="info" bsSize="lg" title="Maintenance" id="indices-maintenance-actions" pullRight>
          <MenuItem eventKey="1" onClick={this._onRecalculateIndexRange}>Recalculate index ranges</MenuItem>
          <MenuItem eventKey="2" onClick={this._onCycleDeflector}>Manually cycle deflector</MenuItem>
        </DropdownButton>
      </ButtonGroup>
    );
  },
});

export default IndicesMaintenanceDropdown;
