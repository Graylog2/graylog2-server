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
import { useState } from 'react';

import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import type { InputStates } from 'stores/inputs/InputStatesStore';
import { useStore } from 'stores/connect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { Input } from 'components/messageloaders/Types';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Button } from 'components/bootstrap';
import { useInputSetupWizard } from 'components/inputs/InputSetupWizard';

type Props = {
  input: Input
}

const InputStateControl = ({ input } : Props) => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const { openWizard } = useInputSetupWizard();

  const inputState = inputStates ? inputStates[input.id] : undefined;

  const inputNodeIds = () => {
    if (!inputState) {
      return [];
    }

    return Object.keys(inputState);
  };

  const isInputRunning = () => {
    const nodeIDs = inputNodeIds();

    if (nodeIDs.length === 0) {
      return false;
    }

    return nodeIDs.some((nodeID) => {
      const nodeState = inputState[nodeID];

      return nodeState.state === 'RUNNING' || nodeState.state === 'STARTING' || nodeState.state === 'FAILING';
    });
  };

  const isInputinSetupMode = () => {
    const nodeIDs = inputNodeIds();

    if (nodeIDs.length === 0) {
      return false;
    }

    return nodeIDs.some((nodeID) => {
      const nodeState = inputState[nodeID];

      return nodeState.state === 'SETUP';
    });
  };

  const startInput = () => {
    setIsLoading(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_START_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'start-input',
    });

    InputStatesStore.start(input)
      .finally(() => {
        setIsLoading(false);
      });
  };

  const stopInput = () => {
    setIsLoading(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_STOP_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'stop-input',
    });

    InputStatesStore.stop(input)
      .finally(() => {
        setIsLoading(false);
      });
  };

  const setupInput = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_SETUP_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'setup-input',
    });

    openWizard({ inputId: input.id });
  };

  if (isInputinSetupMode()) {
    return (
      <Button bsStyle="warning" onClick={setupInput}>
        Setup Input
      </Button>
    );
  }

  if (isInputRunning()) {
    return (
      <Button bsStyle="primary" onClick={stopInput} disabled={isLoading}>
        {isLoading ? 'Stopping...' : 'Stop input'}
      </Button>
    );
  }

  return (
    <Button bsStyle="success" onClick={startInput} disabled={isLoading}>
      {isLoading ? 'Starting...' : 'Start input'}
    </Button>
  );
};

export default InputStateControl;
