import React from 'react';
import { ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';

import ActionsProvider from 'injection/ActionsProvider';
const DeflectorActions = ActionsProvider.getActions('Deflector');
const IndexRangesActions = ActionsProvider.getActions('IndexRanges');

import StoreProvider from 'injection/StoreProvider';
const DeflectorStore = StoreProvider.getStore('Deflector'); // eslint-disable-line no-unused-vars

const IndicesMaintenanceDropdown = React.createClass({
  propTypes: {
    indexSetId: React.PropTypes.string.isRequired,
    indexSet: React.PropTypes.object,
  },

  _onRecalculateIndexRange() {
    if (window.confirm('This will trigger a background system job. Go on?')) {
      // TODO 2.2: Only rebuild index set here? It currently rebuilds all index ranges!
      IndexRangesActions.recalculate();
    }
  },
  _onCycleDeflector() {
    if (window.confirm('Really manually cycle deflector? Follow the documentation link on this page to learn more.')) {
      DeflectorActions.cycle(this.props.indexSetId).then(() => {
        DeflectorActions.list(this.props.indexSetId);
      });
    }
  },
  render() {
    let cycleButton;
    if (this.props.indexSet && this.props.indexSet.writable) {
      cycleButton = <MenuItem eventKey="2" onClick={this._onCycleDeflector}>Manually rotate active write index</MenuItem>;
    }
    return (
      <ButtonGroup>
        <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
          <MenuItem eventKey="1" onClick={this._onRecalculateIndexRange}>Recalculate all index ranges</MenuItem>
          {cycleButton}
        </DropdownButton>
      </ButtonGroup>
    );
  },
});

export default IndicesMaintenanceDropdown;
