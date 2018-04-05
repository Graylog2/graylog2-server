import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { Button, ButtonToolbar } from 'react-bootstrap';

import DebugOverlay from 'enterprise/components/DebugOverlay';
import SaveViewButton from './SaveViewButton';

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
        <ButtonToolbar>
          <Button onClick={this.handleDashboardClick}>Dashboard</Button>
          <SaveViewButton />
          <Button onClick={this.handleDebugOpen}>Debug</Button>
        </ButtonToolbar>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
      </span>
    );
  },
});

export default QueryTabActions;
