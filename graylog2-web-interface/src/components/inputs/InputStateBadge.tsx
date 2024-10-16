import React from 'react';

import { OverlayTrigger, LinkToNode, Spinner } from 'components/common';
import { Label } from 'components/bootstrap';
import InputStateComparator from 'logic/inputs/InputStateComparator';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import type { Input } from 'components/messageloaders/Types';
import { useStore } from 'stores/connect';

type Props = {
  input: Input,
}

const comparator = new InputStateComparator();

type InputState = {
  count: number,
  state:
    | 'RUNNING'
    | 'FAILED'
    | 'STOPPED'
    | 'STARTING'
}
type InputStates = {
  [inputId: string]: InputState,
}

const InputStateBadge = ({ input }: Props) => {
  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const { nodes } = useStore(NodesStore);

  const _labelClassForState = (sortedStates) => {
    const nodesWithKnownState = sortedStates.reduce((numberOfNodes, state) => numberOfNodes + state.count, 0);

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
  };

  const _textForState = (sortedStates) => (input.global
    ? sortedStates.map((state) => `${state.count} ${state.state}`).join(', ')
    : sortedStates[0].state);

  const isLoading = !(inputStates && nodes);

  if (isLoading) {
    return <Spinner />;
  }

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

  const sorted = Object.keys(sortedInputStates).sort(comparator.compare.bind(comparator)).map((state) => ({
    state: state,
    count: sortedInputStates[state].length,
  }));

  if (sorted.length > 0) {
    const popOverText = sorted.map((state) => sortedInputStates[state.state].map((node) => (
      <small><LinkToNode nodeId={node} />: {state.state}<br />
      </small>
    )));

    return (
      <OverlayTrigger trigger="click" placement="bottom" overlay={popOverText} rootClose title={`Input States for ${input.title}`}>
        <Label bsStyle={_labelClassForState(sorted)}
               title="Click to show details"
               bsSize="xsmall"
               style={{ cursor: 'pointer' }}>{_textForState(sorted)}
        </Label>
      </OverlayTrigger>
    );
  }

  const text = input.global || input.node === undefined ? '0 RUNNING' : 'NOT RUNNING';

  return (
    <Label bsStyle="warning" bsSize="xsmall">{text}</Label>
  );
};

export default InputStateBadge;
