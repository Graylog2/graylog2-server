// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';

import Wizard, { type Step } from 'components/common/Wizard';
import Routes from 'routing/Routes';
import history from 'util/History';
import type { LdapCreate } from 'logic/authentication/ldap/types';

import BackendWizardContext, { type WizardStepsState, type WizardFormValues } from './contexts/BackendWizardContext';
import ServerConfiguration from './ServerConfiguration';
import UserSyncSettings from './UserSyncSettings';
import Sidebar from './Sidebar';
import GroupSyncSettings from './GroupSyncSettings';

type Props = {
  initialValues: WizardFormValues,
  initialStepKey: $PropertyType<Step, 'key'>,
  onSubmit: (LdapCreate) => Promise<void>,
  editing: boolean,
  authServiceType: string,
};

const _prepareSubmitPayload = (authServiceType) => ({
  defaultRoles,
  serverUrlHost,
  serverUrlPort,
  systemUserDn,
  systemUserPassword,
  transportSecuriy,
  userFullNameAttribute,
  userNameAttribute,
  userSearchBase,
  userSearchPattern,
  verifyCertificates,
}: WizardFormValues) => {
  const serverUrl = `${new URI('').host(serverUrlHost).port(serverUrlPort).scheme('ldap')}`;

  return {
    title: 'Example Title',
    description: 'Example description',
    default_roles: defaultRoles,
    config: {
      server_urls: [serverUrl],
      system_user_dn: systemUserDn,
      system_password: systemUserPassword,
      transport_security: transportSecuriy,
      type: authServiceType,
      user_full_name_attribute: userFullNameAttribute,
      user_name_attribute: userNameAttribute,
      user_search_base: userSearchBase,
      user_search_pattern: userSearchPattern,
      verify_certificates: verifyCertificates,
    },
  };
};

const BackendWizard = ({ authServiceType, initialValues, initialStepKey, onSubmit, editing }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    activeStepKey: initialStepKey,
    formValues: initialValues,
    prepareSubmitPayload: _prepareSubmitPayload(authServiceType),
  });

  const {
    serverUrlHost,
    serverUrlPort,
    systemUserDn,
    userSearchBase,
    userSearchPattern,
    userNameAttribute,
    userFullNameAttribute,
  } = stepsState.formValues;

  const isServerConfigValid = !!(serverUrlHost && !!serverUrlPort && systemUserDn);
  const isUserSyncSettingValid = !!(userSearchBase && userSearchPattern && userNameAttribute && userFullNameAttribute);
  const disableSubmitAll = !isServerConfigValid || !isUserSyncSettingValid;

  const _handleSubmitAll = () => {
    if (!disableSubmitAll) {
      const payload = stepsState.prepareSubmitPayload(stepsState.formValues);

      onSubmit(payload).then(() => {
        history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
      });
    }
  };

  const _handleStepChange = (stepKey: $PropertyType<Step, 'key'>) => setStepsState({ ...stepsState, activeStepKey: stepKey });

  const _handleFieldUpdate = (event: SyntheticInputEvent<HTMLInputElement> | { target: { value: string, name: string, checked?: boolean } }) => {
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
                             disableSubmitAll={disableSubmitAll}
                             editing={editing} />
      ),
    },
    {
      key: 'userSync',
      title: 'User Synchronisation',
      component: (
        <UserSyncSettings onSubmit={_handleStepChange}
                          onSubmitAll={_handleSubmitAll}
                          disableSubmitAll={disableSubmitAll}
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
                           disableSubmitAll={disableSubmitAll}
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
  initialStepKey: 'serverConfig',
  initialValues: {
    serverUrlHost: 'localhost',
    serverUrlPort: 389,
    transportSecuriy: 'tls',
    verifyCertificates: true,
    userNameAttribute: 'uid',
    userFullNameAttribute: 'cn',
  },
};

BackendWizard.propTypes = {
  initialStepKey: PropTypes.string,
  initialValues: PropTypes.object,
  editing: PropTypes.bool,
};

export default BackendWizard;
