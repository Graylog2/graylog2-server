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

import type { InputSetupWizardStep } from 'components/inputs/InputSetupWizard/types';
import type { StepDataInterface } from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardStepsContext';

export const getStepData = <StepsData extends StepDataInterface>(
  stepsData: StepsData,
  stepName: InputSetupWizardStep,
  key?: string,
) => {
  if (key) return stepsData[stepName] ? stepsData[stepName][key] : undefined;

  return stepsData[stepName];
};

export const getNextStep = (
  orderedSteps: Array<InputSetupWizardStep>,
  activeStep: InputSetupWizardStep,
): InputSetupWizardStep | undefined => {
  const activeStepIndex = orderedSteps.indexOf(activeStep);

  if (activeStepIndex < 0) return undefined;

  return orderedSteps[activeStepIndex + 1];
};

export const checkHasNextStep = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep) => {
  const nextStep = getNextStep(orderedSteps, activeStep);

  return !!nextStep;
};

export const checkHasPreviousStep = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep) => {
  if (orderedSteps.length === 0 || !activeStep) return false;

  const activeStepIndex = orderedSteps.indexOf(activeStep);

  if (activeStepIndex === -1) return false;

  if (activeStepIndex === 0) return false;

  return true;
};

export const updateStepData = <StepsData extends StepDataInterface>(
  stepsData: StepsData,
  stepName: InputSetupWizardStep,
  data: {},
  override: boolean = false,
) => {
  if (override) {
    return { ...stepsData, [stepName]: data };
  }

  return { ...stepsData, [stepName]: { ...stepsData[stepName], ...data } };
};
