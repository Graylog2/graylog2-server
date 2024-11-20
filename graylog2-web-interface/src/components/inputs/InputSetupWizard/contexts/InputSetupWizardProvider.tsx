/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * ThcheckIs program checkIs free software: you can redcheckIstribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as publcheckIshed by MongoDB, Inc.
 *
 * ThcheckIs program checkIs dcheckIstributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with thcheckIs program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useCallback, useMemo, useState } from 'react';

import InputSetupWizardContext from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardContext';
import type { InputSetupWizardStep, StepData, StepsData, WizardData } from 'components/inputs/InputSetupWizard/types';
import { addStepAfter, getNextStep, checkHasPreviousStep, checkIsNextStepDisabled } from 'components/inputs/InputSetupWizard/helpers/stepHelper';

const DEFAULT_ACTIVE_STEP = undefined;
const DEFAULT_WIZARD_DATA = {};
const DEFAULT_STEPS_DATA = {};

const InputSetupWizardProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(DEFAULT_ACTIVE_STEP);
  const [wizardData, setWizardData] = useState<WizardData>(DEFAULT_WIZARD_DATA);
  const [orderedSteps, setOrderedSteps] = useState<Array<InputSetupWizardStep>>([]);
  const [stepsData, setStepsData] = useState<StepsData>(DEFAULT_STEPS_DATA);
  const [show, setShow] = useState<boolean>(false);

  const updateStepData = useCallback(
    (stepName: InputSetupWizardStep, data: StepData) => {
      setStepsData({ ...stepsData, [stepName]: { ...stepsData[stepName], ...data } });
    },
    [stepsData],
  );

  const updateWizardData = useCallback(
    (key: keyof WizardData, value: WizardData[typeof key]) => {
      setWizardData({ ...wizardData, [key]: value });
    },
    [wizardData],
  );

  const clearWizard = useCallback(() => {
    setActiveStep(DEFAULT_ACTIVE_STEP);
    setWizardData(DEFAULT_WIZARD_DATA);
    setStepsData(DEFAULT_STEPS_DATA);
  }, []);

  const closeWizard = useCallback(() => {
    clearWizard();
    setShow(false);
  }, [clearWizard]);

  const openWizard = useCallback((data: WizardData = {}) => {
    setWizardData({ ...wizardData, ...data });
    setShow(true);
  }, [wizardData]);

  const enableNextStep = useCallback((step?: InputSetupWizardStep) => {
    const nextStep = step ?? getNextStep(orderedSteps, activeStep);
    if (!nextStep) return;

    updateStepData(nextStep, { enabled: true });
  }, [updateStepData, orderedSteps, activeStep]);

  const goToNextStep = useCallback((step?: InputSetupWizardStep) => {
    const nextStep = step ?? getNextStep(orderedSteps, activeStep);

    if (step) {
      const newOrderedSteps = addStepAfter(orderedSteps, step, activeStep);
      setOrderedSteps(newOrderedSteps);
    }

    if (!nextStep) return;

    if (checkIsNextStepDisabled(orderedSteps, activeStep, stepsData, nextStep)) return;

    const nextStepIndex = orderedSteps.indexOf(nextStep);

    setActiveStep(orderedSteps[nextStepIndex]);
  }, [activeStep, orderedSteps, stepsData]);

  const goToPreviousStep = useCallback(() => {
    if (!checkHasPreviousStep(orderedSteps, activeStep)) return;

    const previousStepIndex = orderedSteps.indexOf(activeStep) - 1;

    setActiveStep(orderedSteps[previousStepIndex]);
  }, [activeStep, orderedSteps]);

  const value = useMemo(() => ({
    setActiveStep,
    activeStep,
    updateStepData,
    stepsData,
    wizardData,
    updateWizardData,
    show,
    orderedSteps,
    setOrderedSteps,
    enableNextStep,
    goToPreviousStep,
    goToNextStep,
    openWizard,
    closeWizard,
  }), [
    activeStep,
    updateStepData,
    stepsData,
    wizardData,
    updateWizardData,
    show,
    orderedSteps,
    enableNextStep,
    goToPreviousStep,
    goToNextStep,
    openWizard,
    closeWizard,
  ]);

  return (
    <InputSetupWizardContext.Provider value={value}>
      {children}
    </InputSetupWizardContext.Provider>
  );
};

export default InputSetupWizardProvider;
