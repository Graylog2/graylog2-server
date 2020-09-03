// @flow strict
import * as React from 'react';

import Wizard from 'components/common/Wizard';

import StepServerConfiguration from '../AuthenticationCreateSteps/StepServerConfiguration';
import StepUserMapping from '../AuthenticationCreateSteps/StepUserMapping';
import SidebarServerResponse from '../AuthenticationCreateSteps/SidebarServerResponse';
import StepGroupMapping from '../AuthenticationCreateSteps/StepGroupMapping';

const AuthenticationCreateLDAP = () => {
  const activeStep = 'server-configuration';
  const wizardFormValues = {};
  const handleSubmit = () => {};
  const handleStepChange = () => {};
  const handleFieldUpdate = () => {};
  const isDisabledStep = (string) => !!string;
  const wizardSteps = [
    {
      key: 'server-configuration',
      title: 'Server Configuration',
      component: (
        <StepServerConfiguration onSubmit={handleSubmit}
                                 onChange={handleFieldUpdate}
                                 sidebarComponent={<SidebarServerResponse />}
                                 wizardFormValues={wizardFormValues} />
      ),
      disabled: false,
    },
    {
      key: 'user-mapping',
      title: 'User Mapping',
      component: (
        <StepUserMapping onSubmit={handleSubmit}
                         onChange={handleFieldUpdate}
                         sidebarComponent={<SidebarServerResponse />}
                         wizardFormValues={wizardFormValues} />
      ),
      disabled: isDisabledStep('user-mapping'),
    },
    {
      key: 'group-mapping',
      title: 'Group Mapping',
      component: (
        <StepGroupMapping onSubmit={handleSubmit}
                          onChange={handleFieldUpdate}
                          sidebarComponent={<SidebarServerResponse />}
                          wizardFormValues={wizardFormValues} />
      ),
      disabled: isDisabledStep('group-mapping'),
    },
  ];

  return (
    <Wizard horizontal
            justified
            activeStep={activeStep}
            onStepChange={handleStepChange}
            hidePreviousNextButtons
            steps={wizardSteps}>

      Sidebar
    </Wizard>

  );
};

export default AuthenticationCreateLDAP;
