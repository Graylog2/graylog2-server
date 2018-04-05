import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import DebugOverlay from 'enterprise/components/DebugOverlay';
import SaveViewMenuItem from './SaveViewMenuItem';

const QueryTabActions = createReactClass({
  propTypes: {
    onToggleDashboard: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      debugOpen: false,
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

  render() {
    return (
      <span>
        <DropdownButton title="View Actions">
          <MenuItem onSelect={this.handleDashboardClick}>Dashboard</MenuItem>
          <SaveViewMenuItem />
          <MenuItem divider />
          <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
      </span>
    );
  },
});

export default QueryTabActions;
