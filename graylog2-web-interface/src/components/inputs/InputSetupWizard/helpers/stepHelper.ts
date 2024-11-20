import type { InputSetupWizardStep, StepsData } from 'components/inputs/InputSetupWizard/types';

export const getStepData = (stepsData: StepsData, stepName: InputSetupWizardStep, key?: string) => {
  if (key) return stepsData[stepName] ? stepsData[stepName][key] : undefined;

  return stepsData[stepName];
};

export const getNextStep = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep) : InputSetupWizardStep | undefined => {
  const activeStepIndex = orderedSteps.indexOf(activeStep);

  return orderedSteps[activeStepIndex + 1];
};

export const checkIsNextStepDisabled = (orderedSteps: Array<InputSetupWizardStep>, activeStep: InputSetupWizardStep, stepsData: StepsData, step?: InputSetupWizardStep) => {
  const nextStep = step ?? getNextStep(orderedSteps, activeStep);

  return !stepsData[nextStep]?.enabled;
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
