import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Label, OverlayTrigger, Popover } from 'components/graylog';

import StoreProvider from 'injection/StoreProvider';

import { LinkToNode, Spinner } from 'components/common';

import InputStateComparator from 'logic/inputs/InputStateComparator';

const InputStatesStore = StoreProvider.getStore('InputStates');
const NodesStore = StoreProvider.getStore('Nodes');

const InputStateBadge = createReactClass({
  displayName: 'InputStateBadge',

  propTypes: {
    input: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(InputStatesStore), Reflux.connect(NodesStore)],
  comparator: new InputStateComparator(),

  _labelClassForState(sortedStates) {
    const nodesWithKnownState = sortedStates.reduce((numberOfNodes, state) => {
      return numberOfNodes + state.count;
    }, 0);

    if (this.props.input.global && nodesWithKnownState !== Object.keys(this.state.nodes).length) {
      return 'warning';
    }

    const { state } = sortedStates[0];
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
    if (this.props.input.global) {
      return sortedStates.map((state) => `${state.count} ${state.state}`).join(', ');
    }
    return sortedStates[0].state;
  },

  _isLoading() {
    return !(this.state.inputStates && this.state.nodes);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { input } = this.props;
    const inputId = input.id;

    const inputStates = {};
    if (this.state.inputStates[inputId]) {
      Object.keys(this.state.inputStates[inputId]).forEach((node) => {
        const { state } = this.state.inputStates[inputId][node];
        if (!inputStates[state]) {
          inputStates[state] = [];
        }
        inputStates[state].push(node);
      });
    }

    const sorted = Object.keys(inputStates).sort(this.comparator.compare.bind(this.comparator)).map((state) => {
      return { state: state, count: inputStates[state].length };
    });

    if (sorted.length > 0) {
      const popOverText = sorted.map((state) => {
        return inputStates[state.state].map((node) => {
          return <span><LinkToNode nodeId={node} />: {state.state}<br /></span>;
        });
      });
      const popover = (
        <Popover id="inputstate-badge-details" title={`Input States for ${input.title}`} style={{ fontSize: 12 }}>
          {popOverText}
        </Popover>
      );
      return (
        <OverlayTrigger trigger="click" placement="bottom" overlay={popover} rootClose>
          <Label bsStyle={this._labelClassForState(sorted)}
                 title="Click to show details"
                 bsSize="xsmall"
                 style={{ cursor: 'pointer' }}>{this._textForState(sorted)}
          </Label>
        </OverlayTrigger>
      );
    }
    const text = input.global || input.node === undefined ? '0 RUNNING' : 'NOT RUNNING';
    return (
      <Label bsStyle="danger" bsSize="xsmall">{text}</Label>
    );
  },
});

export default InputStateBadge;
