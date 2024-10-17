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

import { Wizard } from 'components/common';
import { INPUT_WIZARD_STEPS } from 'contexts/InputSetupWizardContext';
import useInputSetupWizard from 'hooks/useInputSetupWizard';

import CategoryStep from './CategoryStep';

const InputSetupWizard = () => {
  const { activeStep, setActiveStep } = useInputSetupWizard();

  const steps = [{
    key: INPUT_WIZARD_STEPS.SELECT_CATEGORY,
    title: (
      <>
        Title
      </>
    ),
    component: (
      <CategoryStep />
    ),
  }];

  return (
    <Wizard activeStep={activeStep}
            hidePreviousNextButtons
            horizontal
            justified
            onStepChange={setActiveStep}
            steps={steps} />
  );
};

export default InputSetupWizard;
