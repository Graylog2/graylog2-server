import React from 'react';
import Reflux from 'reflux';
import { Label, OverlayTrigger, Popover } from 'react-bootstrap';

import { InputStatesStore } from 'stores/inputs';

import { Spinner } from 'components/common';

import InputStateComparator from 'components/inputs/InputStateComparator';

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

  _textForState(sortedStates) {
    return sortedStates.map(state => state.count + ' ' + state.state).join(', ');
  },
  render() {
    if (!this.state.inputStates) {
      return <Spinner />;
    }

    const input = this.props.input;
    const inputId = input.input_id;

    const inputStates = {};
    if (this.state.inputStates[inputId]) {
      Object.keys(this.state.inputStates[inputId]).forEach((node) => {
        const state = this.state.inputStates[inputId][node].state;
        if (!inputStates[state]) {
          inputStates[state] = [];
        }
        inputStates[state].push(node);
      });
    }

    const sorted = Object.keys(inputStates).sort(this.comparator.compare.bind(this.comparator)).map(state => {
      return {state: state, count: inputStates[state].length};
    });

    if (sorted.length > 0) {
      const popOverText = sorted.map(state => {
        return state.state + ': ' + inputStates[state.state].join(', ');
      }).join('\n ');
      const popover = (
        <Popover>
          {popOverText}
        </Popover>
      );
      return (
        <OverlayTrigger trigger="hover" placement="bottom" overlay={popover}>
          <Label bsStyle={this._labelClassForState(sorted[0].state)}
                 bsSize="xsmall">{this._textForState(sorted)}</Label>
        </OverlayTrigger>
      );
    } else {
      const text = input.global || input.node === undefined ? "0 RUNNING" : "NOT RUNNING";
      return (
        <Label bsStyle="danger" bsSize="xsmall">{text}</Label>
      );
    }
  },
});

export default InputStateBadge;
