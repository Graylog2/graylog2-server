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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Button } from 'components/bootstrap';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import withTelemetry from 'logic/telemetry/withTelemetry';

function inputStateFilter(state) {
  return state.inputStates ? state.inputStates[this.props.input.id] : undefined;
}

const InputStateControl = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'InputStateControl',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    input: PropTypes.object.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
  },

  mixins: [Reflux.connectFilter(InputStatesStore, 'inputState', inputStateFilter)],

  getInitialState() {
    return {
      loading: false,
    };
  },

  _isInputRunning() {
    if (!this.state.inputState) {
      return false;
    }

    const nodeIDs = Object.keys(this.state.inputState);

    if (nodeIDs.length === 0) {
      return false;
    }

    return nodeIDs.some((nodeID) => {
      const nodeState = this.state.inputState[nodeID];

      return nodeState.state === 'RUNNING' || nodeState.state === 'STARTING' || nodeState.state === 'FAILING';
    });
  },

  _startInput() {
    this.setState({ loading: true });

    this.props.sendTelemetry('click', {
      app_pathname: 'inputs',
      app_action_value: 'start-input',
    });

    InputStatesStore.start(this.props.input)
      .finally(() => {
        this.setState({ loading: false });
      });
  },

  _stopInput() {
    this.setState({ loading: true });

    this.props.sendTelemetry('click', {
      app_pathname: 'inputs',
      app_action_value: 'stop-input',
    });

    InputStatesStore.stop(this.props.input)
      .finally(() => {
        this.setState({ loading: false });
      });
  },

  render() {
    if (this._isInputRunning()) {
      return (
        <Button bsStyle="primary" onClick={this._stopInput} disabled={this.state.loading}>
          {this.state.loading ? 'Stopping...' : 'Stop input'}
        </Button>
      );
    }

    return (
      <Button bsStyle="success" onClick={this._startInput} disabled={this.state.loading}>
        {this.state.loading ? 'Starting...' : 'Start input'}
      </Button>
    );
  },
});

export default withTelemetry(InputStateControl);
