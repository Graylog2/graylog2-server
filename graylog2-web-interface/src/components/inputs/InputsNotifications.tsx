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
import { useEffect, useMemo } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import useInputsStates from 'hooks/useInputsStates';
import type { InputStates, InputState } from 'hooks/useInputsStates';
import { useStore } from 'stores/connect';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';

const INPUT_STATES = {
  FAILED: 'FAILED',
  FAILING: 'FAILING',
  SETUP: 'SETUP',
} as const;
const StyledAlert = styled(Alert)(
  ({ theme }) => css`
    margin-top: 10px;

    i {
      color: ${theme.colors.gray[10]};
    }

    form {
      margin-bottom: 0;
    }
  `,
);

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

const InputsNotifications = () => {
  const { data: inputStates, isLoading } = useInputsStates();
  const inputs = useStore(InputsStore, (state) => state.inputs);

  useEffect(() => {
    InputsActions.list();
  }, []);

  const notifications = useMemo(() => {
    if (isLoading || !inputs || !inputStates) return null;

    return {
      hasStoppedInputs: inputs.some((input) => !inputStates[input.id]),
      hasFailedInputs: hasInputInState(inputStates, [INPUT_STATES.FAILED, INPUT_STATES.FAILING]),
      hasSetupInputs: hasInputInState(inputStates, INPUT_STATES.SETUP),
    };
  }, [inputs, inputStates, isLoading]);

  if (!notifications) {
    return null;
  }

  const { hasStoppedInputs, hasFailedInputs, hasSetupInputs } = notifications;

  if (!hasStoppedInputs && !hasFailedInputs && !hasSetupInputs) {
    return null;
  }

  return (
    <Row className="content">
      <Col md={12}>
        {hasFailedInputs && (
          <StyledAlert bsStyle="danger" title="Some inputs are in failed state.">
            One or more inputs are currently in failed state. Failed or failing inputs will not receive traffic until
            fixed.
          </StyledAlert>
        )}
        {hasSetupInputs && (
          <StyledAlert bsStyle="warning" title="Some inputs are in setup mode.">
            One or more inputs are currently in setup mode. Inputs will not receive traffic until started.
          </StyledAlert>
        )}
        {hasStoppedInputs && (
          <StyledAlert bsStyle="warning" title="Some inputs are stopped.">
            One or more inputs are currently stopped. Stopped Inputs will not receive traffic until started.
          </StyledAlert>
        )}
      </Col>
    </Row>
  );
};

export default InputsNotifications;
