import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';

import { Spinner } from 'components/common';

import { InputStatesStore } from 'stores/inputs';

function inputStateFilter(state) {
  return state.inputStates[this.props.input.id];
}

const InputStateControl = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connectFilter(InputStatesStore, 'inputState', inputStateFilter)],
  _isInputRunning() {
    if (!this.state.inputState) {
      return false;
    }

    const nodeIDs = Object.keys(this.state.inputState);
    let isInputRunning = true;

    for (let i = 0; i < nodeIDs.length && isInputRunning; i++) {
      const nodeState = this.state.inputState[nodeIDs[i]];
      isInputRunning = nodeState.state === 'RUNNING';
    }

    return isInputRunning;
  },
  _startInput() {
    InputStatesStore.start(this.props.input);
  },
  _stopInput() {
    InputStatesStore.stop(this.props.input);
  },
  render() {
    if (this._isInputRunning()) {
      return <Button bsStyle="primary" onClick={this._stopInput}>Stop input</Button>;
    }

    return <Button bsStyle="success" onClick={this._startInput}>Start input</Button>;
  },
});

export default InputStateControl;
