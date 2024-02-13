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
