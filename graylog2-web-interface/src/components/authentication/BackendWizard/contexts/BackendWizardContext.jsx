// @flow strict
import * as React from 'react';

import type { Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type WizardFormValues = any;
export type WizardStepsState = {
  activeStepKey: $PropertyType<Step, 'key'>,
  formValues: WizardFormValues,
  prepareSubmitPayload: WizardFormValues => ({
    title: string,
    description: string,
    config: any,
  }),
};

export type BackendWizardType = WizardStepsState & {
  setStepsState: (BackendWizardType) => void,
};

const initialState = {
  setStepsState: () => {},
  activeStepKey: '',
  formValues: {},
  prepareSubmitPayload: () => {},
};

const BackendWizardContext = React.createContext<BackendWizardType>(initialState);
export default singleton('contexts.systems.authentication.ServiceSteps.', () => BackendWizardContext);
