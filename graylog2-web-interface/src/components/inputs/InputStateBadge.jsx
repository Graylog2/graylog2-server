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
    const { input } = this.props;
    const { nodes } = this.state;
    const nodesWithKnownState = sortedStates.reduce((numberOfNodes, state) => {
      return numberOfNodes + state.count;
    }, 0);

    if (input.global && nodesWithKnownState !== Object.keys(nodes).length) {
      return 'warning';
    }

    const { state } = sortedStates[0];

    switch (state) {
      case 'RUNNING':
        return 'success';
      case 'FAILED':
      case 'STOPPED':
        return 'danger';
      case 'STARTING':
        return 'info';
      default:
        return 'warning';
    }
  },

  _textForState(sortedStates) {
    const { input } = this.props;

    if (input.global) {
      return sortedStates.map((state) => `${state.count} ${state.state}`).join(', ');
    }

    return sortedStates[0].state;
  },

  _isLoading() {
    const { inputStates, nodes } = this.state;

    return !(inputStates && nodes);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { input } = this.props;
    const { inputStates } = this.state;
    const inputId = input.id;
    const sortedInputStates = {};

    if (inputStates[inputId]) {
      Object.keys(inputStates[inputId]).forEach((node) => {
        const { state } = inputStates[inputId][node];

        if (!sortedInputStates[state]) {
          sortedInputStates[state] = [];
        }

        sortedInputStates[state].push(node);
      });
    }

    const sorted = Object.keys(sortedInputStates).sort(this.comparator.compare.bind(this.comparator)).map((state) => {
      return { state: state, count: sortedInputStates[state].length };
    });

    if (sorted.length > 0) {
      const popOverText = sorted.map((state) => {
        return sortedInputStates[state.state].map((node) => {
          return <small><LinkToNode nodeId={node} />: {state.state}<br /></small>;
        });
      });
      const popover = (
        <Popover id="inputstate-badge-details" title={`Input States for ${input.title}`}>
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
      <Label bsStyle="warning" bsSize="xsmall">{text}</Label>
    );
  },
});

export default InputStateBadge;
