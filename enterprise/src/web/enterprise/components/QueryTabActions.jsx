import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import DebugOverlay from 'enterprise/components/DebugOverlay';
import SaveViewModal from './views/SaveViewModal';

const QueryTabActions = createReactClass({
  propTypes: {
    onSaveFinished: PropTypes.func.isRequired,
    onToggleDashboard: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      debugOpen: false,
      saveViewOpen: false,
    };
  },

  handleDashboardClick() {
    this.props.onToggleDashboard();
  },

  handleDebugOpen() {
    this.setState({ debugOpen: true });
  },

  handleDebugClose() {
    this.setState({ debugOpen: false });
  },

  handleSaveView() {
    this.setState({ saveViewOpen: true });
  },

  handleSaveViewClose() {
    this.setState({ saveViewOpen: false });
  },

  handleSaveFinished(view) {
    this.props.onSaveFinished(view);
  },

  render() {
    return (
      <span>
        <DropdownButton title="View Actions">
          <MenuItem onSelect={this.handleDashboardClick}>Dashboard</MenuItem>
          <MenuItem onSelect={this.handleSaveView}>Save</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
        <SaveViewModal show={this.state.saveViewOpen} onClose={this.handleSaveViewClose} onSaveFinished={this.handleSaveFinished}/>
      </span>
    );
  },
});

export default QueryTabActions;
