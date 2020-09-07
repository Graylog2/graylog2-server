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
    formValues: {
      'server-configuration': {
        uriHost: 'localhost',
        uriPort: 389,
        useStartTLS: true,
        trustAllCertificates: false,
      },
      'user-mapping': {

      },
      userMapping: undefined,
    },
  });

  const wizardFormValues = {};

  const _handleStepChange = (stepKey: string) => setStepsState({ ...stepsState, activeStepKey: stepKey });
  const _handleSubmitAll = () => {};

  const _handleFieldUpdate = (stepKey, event, values) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;

    setStepsState({
      ...stepsState,
      formValues: {
        ...stepsState.formValues,
        [stepKey]: {
          ...values,
          [event.target.name]: value,
        },
      },
    });
  };

  const wizardSteps = [
    {
      key: 'server-configuration',
      title: 'Server Configuration',
      component: (
        <StepServerConfiguration onSubmit={_handleStepChange}
                                 onSubmitAll={_handleSubmitAll}
                                 onChange={(event, values) => _handleFieldUpdate('server-configuration', event, values)} />
      ),

    },
    {
      key: 'user-mapping',
      title: 'User Mapping',
      component: (
        <StepUserMapping onSubmit={_handleStepChange}
                         onSubmitAll={_handleSubmitAll}
                         onChange={(event, values) => _handleFieldUpdate('user-mapping', event, values)} />
      ),
    },
    {
      key: 'group-mapping',
      title: 'Group Mapping',
      component: (
        <StepGroupMapping onSubmit={_handleStepChange}
                          onSubmitAll={_handleSubmitAll}
                          onChange={_handleFieldUpdate}
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
