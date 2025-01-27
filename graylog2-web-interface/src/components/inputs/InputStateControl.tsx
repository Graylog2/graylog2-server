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
import { isInputRunning, isInputInSetupMode } from 'components/inputs/helpers/inputState';
import { useStore } from 'stores/connect';
import useFeature from 'hooks/useFeature';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { Input } from 'components/messageloaders/Types';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Button } from 'components/bootstrap';
import { INPUT_SETUP_MODE_FEATURE_FLAG } from 'components/inputs/InputSetupWizard';

type Props = {
  input: Input,
  openWizard: () => void,
}

const InputStateControl = ({ input, openWizard } : Props) => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const inputSetupFeatureFlagIsEnabled = useFeature(INPUT_SETUP_MODE_FEATURE_FLAG);

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

    openWizard();
  };

  if (inputSetupFeatureFlagIsEnabled && isInputInSetupMode(inputStates, input.id)) {
    return (
      <Button bsStyle="warning" onClick={setupInput}>
        Setup Input
      </Button>
    );
  }

  if (isInputRunning(inputStates, input.id)) {
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
