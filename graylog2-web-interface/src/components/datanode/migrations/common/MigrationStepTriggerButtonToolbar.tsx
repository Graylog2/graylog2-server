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

import type { MigrationActions, OnTriggerStepFunction, StepArgs } from 'components/datanode/Types';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { MIGRATION_ACTIONS } from 'components/datanode/Constants';

type Props = {
    nextSteps?: Array<MigrationActions>,
    disabled?: boolean,
    onTriggerStep: OnTriggerStepFunction,
    args?: StepArgs,
    hidden?: boolean,
}

const MigrationStepTriggerButtonToolbar = ({ nextSteps, disabled, onTriggerStep, args, hidden }: Props) => {
  if (hidden) {
    return null;
  }

  return (
    <ButtonToolbar>
      {nextSteps.map((step) => <Button key={step} bsStyle="success" disabled={disabled} onClick={() => onTriggerStep(step, args)}>{MIGRATION_ACTIONS[step]?.label || 'Next'}</Button>)}
    </ButtonToolbar>
  );
};

MigrationStepTriggerButtonToolbar.defaultProps = {
  nextSteps: [],
  disabled: false,
  args: {},
  hidden: false,
};

export default MigrationStepTriggerButtonToolbar;
