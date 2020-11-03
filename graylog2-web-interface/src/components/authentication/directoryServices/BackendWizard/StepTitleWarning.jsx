// @flow strict
import * as React from 'react';

import { Icon } from 'components/common';

type Props = {
  invalidStepKeys: Array<string>,
  stepKey: string,
};

const StepTitleWarning = ({ invalidStepKeys = [], stepKey }: Props) => {
  if (invalidStepKeys.includes(stepKey)) {
    return <><Icon name="exclamation-triangle" />{' '}</>;
  }

  return '';
};

export default StepTitleWarning;
