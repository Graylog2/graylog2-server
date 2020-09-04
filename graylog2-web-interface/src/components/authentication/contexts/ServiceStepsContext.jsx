// @flow strict
import * as React from 'react';

import type { Steps, Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type ServiceSteps = {
  // steps: ?Steps,
  setStepsState: ?(ServiceSteps) => void,
  activeStepKey: $PropertyType<Step, 'key'>,
  formValues: any,
};

export const defaultServiceSteps = {
  // steps: undefined,
  setStepsState: undefined,
  activeStepKey: undefined,
  formValues: {
    serverConfiguration: {},
    userMapping: {},
    groupMaoing: {},
  },
};

const ServiceStepsContext = React.createContext<ServiceSteps>(defaultServiceSteps);
export default singleton('contexts.systems.authentication.ServiceSteps.', () => ServiceStepsContext);
