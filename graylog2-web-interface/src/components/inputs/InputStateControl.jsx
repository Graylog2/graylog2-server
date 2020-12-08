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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Button } from 'components/graylog';
import StoreProvider from 'injection/StoreProvider';

const InputStatesStore = StoreProvider.getStore('InputStates');

function inputStateFilter(state) {
  return state.inputStates ? state.inputStates[this.props.input.id] : undefined;
}

const InputStateControl = createReactClass({
  displayName: 'InputStateControl',

  propTypes: {
    input: PropTypes.object.isRequired,
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

      return nodeState.state === 'RUNNING';
    });
  },

  _startInput() {
    this.setState({ loading: true });

    InputStatesStore.start(this.props.input)
      .finally(() => this.setState({ loading: false }));
  },

  _stopInput() {
    this.setState({ loading: true });

    InputStatesStore.stop(this.props.input)
      .finally(() => this.setState({ loading: false }));
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

export default InputStateControl;
