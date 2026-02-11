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
import { useCallback, useEffect } from 'react';

import type { Input } from 'components/messageloaders/Types';
import useInputsStates from 'hooks/useInputsStates';
import type { InputStates, InputState } from 'hooks/useInputsStates';
import { useStore } from 'stores/connect';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';
import { useQueryParams, ArrayParam, NumberParam, StringParam } from 'routing/QueryParams';

import NotificationBanner from './NotificationBanner';
import type { NotificationItem } from './NotificationBanner';

const INPUT_STATES = {
  FAILED: 'FAILED',
  FAILING: 'FAILING',
  SETUP: 'SETUP',
} as const;

const INPUTS_LIST_REFETCH_INTERVAL = 5000;
const RUNTIME_STATUS_FILTER = 'runtime_status';
const FAILED_MESSAGE = 'Inputs have failed and will not receive traffic until started.';
const SETUP_MESSAGE = 'Inputs currently in setup mode will not receive traffic until started.';
const STOPPED_MESSAGE = 'Inputs currently stopped will not receive traffic until started.';

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
    result.push({ severity: 'danger', message: FAILED_MESSAGE });
  }

  if (hasInputInState(inputStates, INPUT_STATES.SETUP)) {
    result.push({ severity: 'warning', message: SETUP_MESSAGE });
  }

  if (inputs.some((input) => !inputStates[input.id])) {
    result.push({ severity: 'warning', message: STOPPED_MESSAGE });
  }

  return result;
};

const InputsNotifications = () => {
  const { data: inputStates, isLoading } = useInputsStates();
  const inputs = useStore(InputsStore, (state) => state.inputs);
  const [, setQueryParams] = useQueryParams({
    filters: ArrayParam,
    page: NumberParam,
    slice: StringParam,
    sliceCol: StringParam,
  });

  useEffect(() => {
    InputsActions.list();
    const interval = setInterval(InputsActions.list, INPUTS_LIST_REFETCH_INTERVAL);

    return () => clearInterval(interval);
  }, []);

  const applyRuntimeStatusFilter = useCallback((status: 'FAILED' | 'SETUP' | 'NOT_RUNNING') => {
    setQueryParams({
      filters: [`${RUNTIME_STATUS_FILTER}=${status}`],
      page: 1,
      slice: undefined,
      sliceCol: undefined,
    });
  }, [setQueryParams]);

  const items = getNotificationItems(inputs, inputStates, isLoading).map((item) => {
    if (item.message === FAILED_MESSAGE) {
      return {
        ...item,
        id: 'failed',
        message: (
          <>
            {FAILED_MESSAGE}{' '}
            <a href="#" onClick={(event) => { event.preventDefault(); applyRuntimeStatusFilter('FAILED'); }}>
              Show failed inputs
            </a>
            .
          </>
        ),
      };
    }

    if (item.message === SETUP_MESSAGE) {
      return {
        ...item,
        id: 'setup',
        message: (
          <>
            {SETUP_MESSAGE}{' '}
            <a href="#" onClick={(event) => { event.preventDefault(); applyRuntimeStatusFilter('SETUP'); }}>
              Show inputs in setup mode
            </a>
            .
          </>
        ),
      };
    }

    if (item.message === STOPPED_MESSAGE) {
      return {
        ...item,
        id: 'stopped',
        message: (
          <>
            {STOPPED_MESSAGE}{' '}
            <a href="#" onClick={(event) => { event.preventDefault(); applyRuntimeStatusFilter('NOT_RUNNING'); }}>
              Show stopped inputs
            </a>
            .
          </>
        ),
      };
    }

    return item;
  });

  return <NotificationBanner title="Warning" items={items} />;
};

export default InputsNotifications;
