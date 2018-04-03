import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { Button, ButtonToolbar } from 'react-bootstrap';

import DebugOverlay from 'enterprise/components/DebugOverlay';

const QueryTabActions = createReactClass({
  getInitialState() {
    return {
      debugOpen: false,
    };
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
          <Button onClick={this.handleDebugOpen}>Debug</Button>
        </ButtonToolbar>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
      </span>
    );
  },
});

export default QueryTabActions;
