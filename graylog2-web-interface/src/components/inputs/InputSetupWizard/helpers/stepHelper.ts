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

import type { InputSetupWizardStep, StepsConfig, StepsData } from 'components/inputs/InputSetupWizard/types';

export const getStepConfigOrData = (configOrData: StepsConfig | StepsData, stepName: InputSetupWizardStep, key?: string) => {
  if (key) return configOrData[stepName] ? configOrData[stepName][key] : undefined;

  return configOrData[stepName];
};

export const getNextStep = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep) : InputSetupWizardStep | undefined => {
  const activeStepIndex = orderedSteps.indexOf(activeStep);

  if (activeStepIndex < 0) return undefined;

  return orderedSteps[activeStepIndex + 1];
};

export const checkIsNextStepDisabled = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep, stepsConfig: StepsConfig, step?: InputSetupWizardStep) => {
  const nextStep = step ?? getNextStep(orderedSteps, activeStep);

  return !stepsConfig[nextStep]?.enabled;
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

export const addStepAfter = (orderedSteps: Array<InputSetupWizardStep>, step: InputSetupWizardStep, setAfterStep?: InputSetupWizardStep) : Array<InputSetupWizardStep> => {
  if (!setAfterStep) return [...orderedSteps, step];

  const setAfterStepIndex = orderedSteps.indexOf(setAfterStep);

  if (setAfterStepIndex === -1) return orderedSteps;

  const newOrderedSteps = [
    ...orderedSteps.slice(0, setAfterStepIndex + 1),
    step,
    ...orderedSteps.slice(setAfterStepIndex + 1),
  ];

  return newOrderedSteps;
};

export const updateStepConfigOrData = (configOrData: StepsConfig | StepsData, stepName: InputSetupWizardStep, data: object = {}, override: boolean = false) : StepsConfig | StepsData => {
  if (!stepName) return {};

  if (!configOrData) return { [stepName]: data };

  if (override) {
    return { ...configOrData, [stepName]: data };
  }

  return { ...configOrData, [stepName]: { ...configOrData[stepName], ...data } };
};

export const enableNextStep = (
  orderedSteps: Array<InputSetupWizardStep>,
  activeStep: InputSetupWizardStep,
  stepsConfig: StepsConfig,
  step?: InputSetupWizardStep,
) : StepsConfig => {
  const nextStep = step ?? getNextStep(orderedSteps, activeStep);

  if (!nextStep) return stepsConfig;

  return updateStepConfigOrData(stepsConfig, nextStep, { enabled: true });
};

export const disableNextStep = (
  orderedSteps: Array<InputSetupWizardStep>,
  activeStep: InputSetupWizardStep,
  stepsConfig: StepsConfig,
  step?: InputSetupWizardStep,
) : StepsConfig => {
  const nextStep = step ?? getNextStep(orderedSteps, activeStep);

  if (!nextStep) return stepsConfig;

  return updateStepConfigOrData(stepsConfig, nextStep, { enabled: false });
};
