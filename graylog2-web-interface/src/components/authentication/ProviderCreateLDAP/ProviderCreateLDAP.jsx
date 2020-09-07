// @flow strict
import * as React from 'react';
import { useState } from 'react';

import Wizard from 'components/common/Wizard';

import ServiceStepsContext from '../contexts/ServiceStepsContext';
import StepServerConfiguration from '../ProviderCreateSteps/StepServerConfiguration';
import StepUserMapping from '../ProviderCreateSteps/StepUserMapping';
import SidebarServerResponse from '../ProviderCreateSteps/SidebarServerResponse';
import StepGroupMapping from '../ProviderCreateSteps/StepGroupMapping';

const ProviderCreateLDAP = () => {
  const [stepsState, setStepsState] = useState({
    activeStepKey: 'server-configuration',
    forms: {
      serverConfig: undefined,
      userMapping: undefined,
    },
  });

  const wizardFormValues = {};

  const _handleStepChange = (stepKey: string) => setStepsState({ ...stepsState, activeStepKey: stepKey });
  const _handleSubmitAll = () => {};
  const handleFieldUpdate = () => {};

  const wizardSteps = [
    {
      key: 'server-configuration',
      title: 'Server Configuration',
      component: (
        <StepServerConfiguration onSubmit={_handleStepChange}
                                 onSubmitAll={_handleSubmitAll}
                                 onChange={handleFieldUpdate} />
      ),

    },
    {
      key: 'user-mapping',
      title: 'User Mapping',
      component: (
        <StepUserMapping onSubmit={_handleStepChange}
                         onSubmitAll={_handleSubmitAll}
                         onChange={handleFieldUpdate} />
      ),
    },
    {
      key: 'group-mapping',
      title: 'Group Mapping',
      component: (
        <StepGroupMapping onSubmit={_handleStepChange}
                          onSubmitAll={_handleSubmitAll}
                          onChange={handleFieldUpdate}
                          wizardFormValues={wizardFormValues} />
      ),
    },
  ];

  return (
    <ServiceStepsContext.Provider value={{ ...stepsState, setStepsState }}>
      <ServiceStepsContext.Consumer>
        {({ activeStepKey: activeStep }) => {
          return (
            <Wizard horizontal
                    justified
                    activeStep={activeStep}
                    onStepChange={_handleStepChange}
                    hidePreviousNextButtons
                    steps={wizardSteps}>
              <SidebarServerResponse />
            </Wizard>
          );
        }}
      </ServiceStepsContext.Consumer>
    </ServiceStepsContext.Provider>
  );
};

export default ProviderCreateLDAP;
