import React from 'react';
import Reflux from 'reflux';
import { Label } from 'react-bootstrap';

import { InputStatesStore } from 'stores/inputs';

import { Spinner } from 'components/common';

import { InputStateComparator } from 'components/inputs';

const InputStateBadge = React.createClass({
  mixins: [Reflux.connect(InputStatesStore)],
  propTypes: {
    input: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return {};
  },
  comparator: new InputStateComparator(),
  _labelClassForState(state) {
    switch (state) {
      case 'RUNNING':
        return 'success';
      case 'FAILED':
        return 'danger';
      case 'STARTING':
        return 'info';
      default:
        return 'warning';
    }
  },

  render() {
    if (!this.state.inputStates) {
      return <Spinner />;
    }

    const inputStates = Object.keys(this.state.inputStates[this.props.input.id]).map((node) => {
      return {node: node, input: input.id, state: this.state.inputStates[this.props.input.id][node].state};
    });

    return (
      <Label bsStyle={this._labelClassForState(this.props.input.state)}
             bsSize="xsmall">{this.props.input.state.toLowerCase()}</Label>
    );
  },
});

export default InputStateBadge;
