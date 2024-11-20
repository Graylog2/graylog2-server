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

import { singleton } from 'logic/singleton';
import type { InputSetupWizardStep, WizardData, StepData, StepsData } from 'components/inputs/InputSetupWizard/types';

type InputSetupWizardContextType = {
  activeStep: InputSetupWizardStep | undefined,
  setActiveStep: (InputSetupWizardStep) => void,
  stepsData: StepsData,
  updateStepData: (stepName: InputSetupWizardStep, data: StepData) => void,
  wizardData: WizardData,
  updateWizardData: (key: keyof WizardData, value: WizardData[typeof key]) => void,
  orderedSteps: Array<InputSetupWizardStep>,
  setOrderedSteps: (steps: Array<InputSetupWizardStep>) => void,
  show: boolean,
  enableNextStep: (step?: InputSetupWizardStep) => void,
  goToPreviousStep: () => void,
  goToNextStep: (step?: InputSetupWizardStep) => void,
  openWizard: (data?: WizardData) => void,
  closeWizard: () => void,
};

const InputSetupWizardContext = React.createContext<InputSetupWizardContextType | undefined>(undefined);

export default singleton('contexts.InputSetupWizardContext', () => InputSetupWizardContext);
