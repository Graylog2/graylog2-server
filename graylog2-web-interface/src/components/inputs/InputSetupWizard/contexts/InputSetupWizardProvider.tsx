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
import { useCallback, useMemo, useState } from 'react';

import InputSetupWizardContext from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardContext';
import type { InputSetupWizardStep, WizardData } from 'components/inputs/InputSetupWizard/types';
import { INPUT_WIZARD_FLOWS } from 'components/inputs/InputSetupWizard/types';
import { getNextStep, checkHasPreviousStep } from 'components/inputs/InputSetupWizard/helpers/stepHelper';

const DEFAULT_ACTIVE_STEP = undefined;
const DEFAULT_WIZARD_DATA = {
  flow: INPUT_WIZARD_FLOWS.NON_ILLUMINATE,
};

const InputSetupWizardProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(DEFAULT_ACTIVE_STEP);
  const [wizardData, setWizardData] = useState<WizardData>(DEFAULT_WIZARD_DATA);
  const [orderedSteps, setOrderedSteps] = useState<Array<InputSetupWizardStep>>([]);

  const goToNextStep = useCallback(() => {
    const nextStep = getNextStep(orderedSteps, activeStep);

    if (!nextStep) return;

    const nextStepIndex = orderedSteps.indexOf(nextStep);

    setActiveStep(orderedSteps[nextStepIndex]);
  }, [activeStep, orderedSteps]);

  const goToPreviousStep = useCallback(() => {
    if (!checkHasPreviousStep(orderedSteps, activeStep)) return;

    const previousStepIndex = orderedSteps.indexOf(activeStep) - 1;

    setActiveStep(orderedSteps[previousStepIndex]);
  }, [activeStep, orderedSteps]);

  const value = useMemo(
    () => ({
      setActiveStep,
      activeStep,
      wizardData,
      setWizardData,
      orderedSteps,
      setOrderedSteps,
      goToPreviousStep,
      goToNextStep,
    }),
    [activeStep, wizardData, orderedSteps, goToPreviousStep, goToNextStep],
  );

  return <InputSetupWizardContext.Provider value={value}>{children}</InputSetupWizardContext.Provider>;
};

export default InputSetupWizardProvider;
