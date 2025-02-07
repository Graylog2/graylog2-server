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

import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import InputSetupWizardContext from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardContext';
import type { InputSetupWizardStep, WizardData, StepsConfig } from 'components/inputs/InputSetupWizard/types';
import { getNextStep, checkHasPreviousStep, checkIsNextStepDisabled } from 'components/inputs/InputSetupWizard/helpers/stepHelper';

const DEFAULT_ACTIVE_STEP = undefined;
const DEFAULT_WIZARD_DATA = {};
const DEFAULT_ORDERED_STEPS = [INPUT_WIZARD_STEPS.SETUP_ROUTING, INPUT_WIZARD_STEPS.START_INPUT, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

const InputSetupWizardProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(DEFAULT_ACTIVE_STEP);
  const [wizardData, setWizardData] = useState<WizardData>(DEFAULT_WIZARD_DATA);
  const [orderedSteps, setOrderedStepsState] = useState<Array<InputSetupWizardStep>>(DEFAULT_ORDERED_STEPS);
  const [stepsConfig, setStepsConfig] = useState<StepsConfig>({});

  const setOrderedSteps = useCallback((steps: Array<InputSetupWizardStep>) => {
    setOrderedStepsState([...steps, ...DEFAULT_ORDERED_STEPS]);
  }, [setOrderedStepsState])

  const goToNextStep = useCallback(() => {
    const nextStep = getNextStep(orderedSteps, activeStep);

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
    setOrderedSteps,
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
