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
import type { InputStates, InputState } from 'stores/inputs/InputStatesStore';

const inputHasSomeState = (inputStates: InputStates, inputId: string, states: Array<InputState>) : boolean => {
  if (!inputStates) return false;
  const inputState = inputStates[inputId];
  if (!inputState) return false;

  const nodeIds = Object.keys(inputState);

  if (nodeIds.length === 0) {
    return false;
  }

  return nodeIds.some((nodeID) => {
    const nodeState = inputState[nodeID];

    return states.some((state) => nodeState.state === state);
  });
};

export const isInputRunning = (inputStates: InputStates, inputId: string) => inputHasSomeState(inputStates, inputId, ['RUNNING', 'STARTING', 'FAILING']);
export const isInputInSetupMode = (inputStates: InputStates, inputId: string) => inputHasSomeState(inputStates, inputId, ['SETUP']);
