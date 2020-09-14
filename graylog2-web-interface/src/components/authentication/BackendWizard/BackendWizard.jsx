// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';

import Wizard, { type Step } from 'components/common/Wizard';
import Routes from 'routing/Routes';
import history from 'util/History';

import BackendWizardContext, { type WizardStepsState, type WizardFormValues } from './contexts/BackendWizardContext';
import ServerConfiguration from './ServerConfiguration';
import UserSyncSettings from './UserSyncSettings';
import Sidebar from './Sidebar';
import GroupSyncSettings from './GroupSyncSettings';

import type { LdapCreate } from '../ldap/types';

type Props = {
  initialValues: WizardFormValues,
  initialStep: $PropertyType<Step, 'key'>,
  onSubmit: (LdapCreate) => Promise<void>,
  editing: boolean,
  authServiceType: string,
};

const BackendWizard = ({ authServiceType, initialValues, initialStep, onSubmit, editing }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    activeStepKey: initialStep,
    formValues: initialValues,
  });

  const {
    defaultRoles,
    serverUriHost,
    serverUriPort,
    systemUsername,
    systemPassword,
    userSearchBase,
    userSearchPattern,
    displayNameAttribute,
    trustAllCertificates,
    useStartTls,
    useSsl,
  } = stepsState.formValues;

  const isServerConfigValid = !!(serverUriHost && !!serverUriPort && systemUsername && systemPassword);
  const isUserSyncSettingValid = !!(userSearchBase && userSearchPattern && displayNameAttribute);

  const _handleStepChange = (stepKey: $PropertyType<Step, 'key'>) => setStepsState({ ...stepsState, activeStepKey: stepKey });

  const _handleSubmitAll = () => {
    if (isServerConfigValid && isUserSyncSettingValid) {
      const serverUri = `${new URI('').host(serverUriHost).port(serverUriPort).scheme('ldap')}`;
      const payload = {
        title: 'Example Title',
        description: 'Example description',
        config: {
          type: authServiceType,
          default_roles: defaultRoles,
          display_name_attribute: displayNameAttribute,
          server_uri: serverUri,
          system_username: systemUsername,
          trust_all_certificates: trustAllCertificates,
          user_search_base: userSearchBase,
          user_search_pattern: userSearchPattern,
          use_start_tls: useStartTls,
          use_ssl: useSsl,
        },
      };

      onSubmit(payload).then(() => {
        history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
      });
    }
  };

  const _handleFieldUpdate = (event: SyntheticInputEvent<HTMLInputElement>) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;

    setStepsState({
      ...stepsState,
      formValues: {
        ...stepsState.formValues,
        [event.target.name]: value,
      },
    });
  };

  const wizardSteps = [
    {
      key: 'serverConfig',
      title: 'Server Configuration',
      component: (
        <ServerConfiguration onSubmit={_handleStepChange}
                             onSubmitAll={_handleSubmitAll}
                             onChange={_handleFieldUpdate}
                             editing={editing} />
      ),
    },
    {
      key: 'userSync',
      title: 'User Synchronisation',
      component: (
        <UserSyncSettings onSubmit={_handleStepChange}
                          onSubmitAll={_handleSubmitAll}
                          onChange={_handleFieldUpdate} />
      ),
      disabled: !isServerConfigValid,
    },
    {
      key: 'groupSync',
      title: 'Group Synchronisation',
      component: (
        <GroupSyncSettings onSubmit={_handleStepChange}
                           onSubmitAll={_handleSubmitAll}
                           onChange={_handleFieldUpdate} />
      ),
      disabled: !isUserSyncSettingValid,
    },
  ];

  return (
    <BackendWizardContext.Provider value={{ ...stepsState, setStepsState }}>
      <BackendWizardContext.Consumer>
        {({ activeStepKey: activeStep }) => (
          <Wizard horizontal
                  justified
                  activeStep={activeStep}
                  onStepChange={_handleStepChange}
                  hidePreviousNextButtons
                  steps={wizardSteps}>
            <Sidebar />
          </Wizard>
        )}
      </BackendWizardContext.Consumer>
    </BackendWizardContext.Provider>
  );
};

BackendWizard.defaultProps = {
  editing: false,
  initialStep: 'serverConfig',
  initialValues: {
    serverUriHost: 'localhost',
    serverUriPort: 389,
    useStartTls: true,
    trustAllCertificates: false,
  },
};

BackendWizard.propTypes = {
  initialStep: PropTypes.string,
  initialValues: PropTypes.object,
  editing: PropTypes.bool,
};

export default BackendWizard;
