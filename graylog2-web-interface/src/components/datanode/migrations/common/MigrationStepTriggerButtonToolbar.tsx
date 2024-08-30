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
import React from 'react';
import styled, { css } from 'styled-components';

import type { MigrationActions, OnTriggerStepFunction, StepArgs } from 'components/datanode/Types';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { MIGRATION_ACTIONS } from 'components/datanode/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';

const StyledButtonToolbar = styled(ButtonToolbar)(({ theme }) => css`
  margin-top: ${theme.spacings.md};
  flex-wrap: wrap;
`);

const getSortedNextSteps = (nextSteps: MigrationActions[]) => nextSteps.reduce((sortedNextSteps, step) => {
  if (!MIGRATION_ACTIONS[step]?.label) {
    return [step, ...sortedNextSteps];
  }

  sortedNextSteps.push(step);

  return sortedNextSteps;
}, []);

type Props = {
    nextSteps?: Array<MigrationActions>,
    disabled?: boolean,
    onTriggerStep: OnTriggerStepFunction,
    args?: StepArgs,
    hidden?: boolean,
    children?: React.ReactNode,
}

const handleTelemetry = (sendTelemetry: (eventType: TelemetryEventType, event: TelemetryEvent) => void) => {
  // use state and step to trigger the right event
};

const MigrationStepTriggerButtonToolbar = ({ nextSteps, disabled, onTriggerStep, args, hidden, children }: Props) => {
  const sendTelemetry = useSendTelemetry();

  if (hidden) {
    return null;
  }

  const handleButtonClick = (step: MigrationActions) => {
    handleTelemetry(sendTelemetry);
    onTriggerStep(step, args);
  };

  return (
    <StyledButtonToolbar>
      {getSortedNextSteps(nextSteps).map((step, index) => <Button key={step} bsStyle={index ? 'default' : 'success'} bsSize="small" disabled={disabled} onClick={() => handleButtonClick(step)}>{MIGRATION_ACTIONS[step]?.label || 'Next'}</Button>)}
      {children}
    </StyledButtonToolbar>
  );
};

MigrationStepTriggerButtonToolbar.defaultProps = {
  nextSteps: [],
  disabled: false,
  args: {},
  hidden: false,
  children: undefined,
};

export default MigrationStepTriggerButtonToolbar;
