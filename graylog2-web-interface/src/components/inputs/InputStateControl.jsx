import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const InputStatesStore = StoreProvider.getStore('InputStates');

function inputStateFilter(state) {
  return state.inputStates ? state.inputStates[this.props.input.id] : undefined;
}

const InputStateControl = React.createClass({
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
