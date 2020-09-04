// @flow strict
import * as React from 'react';
import { useRef, useState } from 'react';

import Wizard from 'components/common/Wizard';

import ServiceStepsContext from '../contexts/ServiceStepsContext';
import StepServerConfiguration from '../ProviderCreateSteps/StepServerConfiguration';
import StepUserMapping from '../ProviderCreateSteps/StepUserMapping';
import SidebarServerResponse from '../ProviderCreateSteps/SidebarServerResponse';
import StepGroupMapping from '../ProviderCreateSteps/StepGroupMapping';

const ProviderCreateLDAP = () => {
  const serverConfigForm = useRef();
  const userMappingForm = useRef();
  const [stepsState, setStepsState] = useState({
    activeStepKey: 'server-configuration',
    formValues: {
      serverConfiguration: serverConfigForm?.current?.value,
      userMapping: userMappingForm?.current?.value,
    },
  });

  const wizardFormValues = {};
  const handleSubmit = () => {};
  const handleStepChange = () => {};
  const handleFieldUpdate = () => {};
  const wizardSteps = [
    {
      key: 'server-configuration',
      title: 'Server Configuration',
      component: (
        <StepServerConfiguration onSubmit={handleSubmit}
                                 onChange={handleFieldUpdate}
                                 formRef={serverConfigForm} />
      ),

    },
    {
      key: 'user-mapping',
      title: 'User Mapping',
      component: (
        <StepUserMapping onSubmit={handleSubmit}
                         onChange={handleFieldUpdate}
                         formRef={userMappingForm} />
      ),
    },
    {
      key: 'group-mapping',
      title: 'Group Mapping',
      component: (
        <StepGroupMapping onSubmit={handleSubmit}
                          onChange={handleFieldUpdate}
                          wizardFormValues={wizardFormValues} />
      ),
    },
  ];

  return (
    <ServiceStepsContext.Provider value={{ ...stepsState, setStepsState }}>
      <ServiceStepsContext.Consumer>
        {({ activeStepKey: activeStep, formValues }) => {
          return (
            <Wizard horizontal
                    justified
                    activeStep={activeStep}
                    onStepChange={handleStepChange}
                    hidePreviousNextButtons
                    steps={wizardSteps}>
              <SidebarServerResponse formValues={formValues} />
            </Wizard>
          );
        }}
      </ServiceStepsContext.Consumer>
    </ServiceStepsContext.Provider>
  );
};

export default ProviderCreateLDAP;
