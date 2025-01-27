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
import type { InputSetupWizardStep, WizardData, StepsConfig } from 'components/inputs/InputSetupWizard/types';
import { addStepAfter, getNextStep, checkHasPreviousStep, checkIsNextStepDisabled } from 'components/inputs/InputSetupWizard/helpers/stepHelper';

const DEFAULT_ACTIVE_STEP = undefined;
const DEFAULT_WIZARD_DATA = {};

const InputSetupWizardProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(DEFAULT_ACTIVE_STEP);
  const [wizardData, setWizardData] = useState<WizardData>(DEFAULT_WIZARD_DATA);
  const [orderedSteps, setOrderedSteps] = useState<Array<InputSetupWizardStep>>([]);
  const [stepsConfig, setStepsConfig] = useState<StepsConfig>({});

  const goToNextStep = useCallback((step?: InputSetupWizardStep) => {
    const nextStep = step ?? getNextStep(orderedSteps, activeStep);

    if (step) {
      const newOrderedSteps = addStepAfter(orderedSteps, step, activeStep);
      setOrderedSteps(newOrderedSteps);
    }

    if (!nextStep) return;

    if (checkIsNextStepDisabled(orderedSteps, activeStep, stepsConfig, nextStep)) return;

    const nextStepIndex = orderedSteps.indexOf(nextStep);

    setActiveStep(orderedSteps[nextStepIndex]);
  }, [activeStep, orderedSteps, stepsConfig]);

  const goToPreviousStep = useCallback(() => {
    if (!checkHasPreviousStep(orderedSteps, activeStep)) return;

    const previousStepIndex = orderedSteps.indexOf(activeStep) - 1;

    setActiveStep(orderedSteps[previousStepIndex]);
  }, [activeStep, orderedSteps]);

  const value = useMemo(() => ({
    setActiveStep,
    activeStep,
    wizardData,
    setWizardData,
    orderedSteps,
    setOrderedSteps,
    stepsConfig,
    setStepsConfig,
    goToPreviousStep,
    goToNextStep,
  }), [
    activeStep,
    wizardData,
    orderedSteps,
    stepsConfig,
    goToPreviousStep,
    goToNextStep,
  ]);

  return (
    <InputSetupWizardContext.Provider value={value}>
      {children}
    </InputSetupWizardContext.Provider>
  );
};

export default InputSetupWizardProvider;
