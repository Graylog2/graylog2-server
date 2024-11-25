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
import type { InputSetupWizardStep, StepsData, WizardData } from 'components/inputs/InputSetupWizard/types';

const DEFAULT_ACTIVE_STEP = undefined;
const DEFAULT_WIZARD_DATA = {};
const DEFAULT_STEPS_DATA = {};

const InputSetupWizardProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(DEFAULT_ACTIVE_STEP);
  const [wizardData, setWizardData] = useState<WizardData>(DEFAULT_WIZARD_DATA);
  const [stepsData, setStepsData] = useState<StepsData>(DEFAULT_STEPS_DATA);
  const [show, setShow] = useState<boolean>(false);

  const setStepData = useCallback(
    (stepName: InputSetupWizardStep, data: object) => {
      setStepsData({ ...stepsData, [stepName]: data });
    },
    [stepsData],
  );

  const getStepData = useCallback((stepName: InputSetupWizardStep) => (stepsData[stepName]), [stepsData]);

  const setWizardDataAttribute = useCallback(
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

  const value = useMemo(() => ({
    setActiveStep,
    activeStep,
    getStepData,
    setStepData,
    wizardData,
    setWizardDataAttribute,
    show,
    openWizard,
    closeWizard,
  }), [
    setActiveStep,
    activeStep,
    getStepData,
    setStepData,
    wizardData,
    setWizardDataAttribute,
    show,
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
