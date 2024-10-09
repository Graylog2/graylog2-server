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

import { singleton } from 'logic/singleton';

export const INPUT_WIZARD_STEPS = {
  SELECT_CATEGORY: 'SELECT_CATEGORY',
  PREFLIGHT: 'PREFLIGHT',
  ACTIVATE_ILLUMINATE: 'ACTIVATE_ILLUMINATE',
  TEST_INPUT: 'TEST_INPUT',
  SETUP_ROUTING: 'SETUP_ROUTING',
  CREATE_STREAM: 'CREATE_STREAM',
  COMPLETE: 'COMPLETE',
} as const;

export type InputSetupWizardStep = typeof INPUT_WIZARD_STEPS[keyof typeof INPUT_WIZARD_STEPS]

export type StepsData = {
  [key in InputSetupWizardStep]?: any
}

export type WizardData = {
  inputId?: string
}

type InputSetupWizardContextType = {
  activeStep: InputSetupWizardStep,
  setActiveStep: (InputSetupWizardStep) => void,
  stepData: (stepName: InputSetupWizardStep) => object | undefined;
  setStepData: (stepName: InputSetupWizardStep, data: object) => void,
  wizardData: WizardData,
  setWizardDataAttribute: (key: keyof WizardData, value: WizardData[typeof key]) => void,
};

const InputSetupWizardContext = React.createContext<InputSetupWizardContextType | undefined>(undefined);

export default singleton('contexts.InputSetupWizardContext', () => InputSetupWizardContext);
