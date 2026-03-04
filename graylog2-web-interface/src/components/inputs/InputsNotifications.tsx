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
import * as React from 'react';
import { useEffect } from 'react';

import type { Input } from 'components/messageloaders/Types';
import useInputsStates from 'hooks/useInputsStates';
import type { InputStates, InputState } from 'hooks/useInputsStates';
import { useStore } from 'stores/connect';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';

import NotificationBanner from './NotificationBanner';
import type { NotificationItem } from './NotificationBanner';

const INPUT_STATES = {
  FAILED: 'FAILED',
  FAILING: 'FAILING',
  SETUP: 'SETUP',
} as const;

const INPUTS_LIST_REFETCH_INTERVAL = 5000;

const hasInputInState = (inputStates: InputStates, targetStates: InputState | Array<InputState>) => {
  const statesToCheck = Array.isArray(targetStates) ? targetStates : [targetStates];

  for (const nodeStates of Object.values(inputStates)) {
    for (const inputState of Object.values(nodeStates)) {
      if (statesToCheck.includes(inputState.state)) {
        return true;
      }
    }
  }

  return false;
};

const getNotificationItems = (
  inputs: Array<Input> | undefined,
  inputStates: InputStates | undefined,
  isLoading: boolean,
): Array<NotificationItem> => {
  if (isLoading || !inputs || !inputStates) return [];

  const result: Array<NotificationItem> = [];

  if (hasInputInState(inputStates, [INPUT_STATES.FAILED, INPUT_STATES.FAILING])) {
    result.push({ severity: 'danger', message: 'in failed state. Failed or failing inputs will not receive traffic until fixed.' });
  }

  if (hasInputInState(inputStates, INPUT_STATES.SETUP)) {
    result.push({ severity: 'warning', message: 'in setup mode. Inputs will not receive traffic until started.' });
  }

  if (inputs.some((input) => !inputStates[input.id])) {
    result.push({ severity: 'warning', message: 'stopped. Stopped Inputs will not receive traffic until started.' });
  }

  return result;
};

const InputsNotifications = () => {
  const { data: inputStates, isLoading } = useInputsStates();
  const inputs = useStore(InputsStore, (state) => state.inputs);

  useEffect(() => {
    InputsActions.list();
    const interval = setInterval(InputsActions.list, INPUTS_LIST_REFETCH_INTERVAL);

    return () => clearInterval(interval);
  }, []);

  const items = getNotificationItems(inputs, inputStates, isLoading);

  return <NotificationBanner title="One or more inputs are currently" items={items} />;
};

export default InputsNotifications;
