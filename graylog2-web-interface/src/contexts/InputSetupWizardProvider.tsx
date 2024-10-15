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

import InputSetupWizardContext, { INPUT_WIZARD_STEPS } from 'contexts/InputSetupWizardContext';
import type { InputSetupWizardStep, StepsData, WizardData } from 'contexts/InputSetupWizardContext';

const InputSetupWizardProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const [activeStep, setActiveStep] = useState<InputSetupWizardStep>(INPUT_WIZARD_STEPS[0]);
  const [wizardData, setWizardData] = useState<WizardData>({});
  const [stepsData, setStepsData] = useState<StepsData>({});

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

  const value = useMemo(() => ({
    setActiveStep,
    activeStep,
    getStepData,
    setStepData,
    wizardData,
    setWizardDataAttribute,
  }), [
    setActiveStep,
    activeStep,
    getStepData,
    setStepData,
    wizardData,
    setWizardDataAttribute,
  ]);

    <InputSetupWizardContext.Provider value={value}>
      {children}
    </InputSetupWizardContext.Provider>;
};

export default InputSetupWizardProvider;
