// @flow strict
import * as React from 'react';

import type { Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type ServiceSteps = {
  // steps: ?Steps,
  setStepsState: ?(any) => void,
  activeStepKey: $PropertyType<Step, 'key'>,
  forms: any,
};

export const defaultServiceSteps = {
  // steps: undefined,
  setStepsState: undefined,
  activeStepKey: undefined,
  forms: {
    serverConfig: {},
    userMapping: {},
  },
};

const ServiceStepsContext = React.createContext<ServiceSteps>(defaultServiceSteps);
export default singleton('contexts.systems.authentication.ServiceSteps.', () => ServiceStepsContext);
