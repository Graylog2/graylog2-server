// @flow strict
import * as React from 'react';
import { useState } from 'react';
import URI from 'urijs';

import Wizard, { type Step } from 'components/common/Wizard';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
import history from 'util/History';

import ServiceStepsContext from '../contexts/ServiceStepsContext';
import ServerConfiguration from '../ServiceCreateSteps/ServerConfiguration';
import UserSyncSettings from '../ServiceCreateSteps/UserSyncSettings';
import SidebarServerResponse from '../ServiceCreateSteps/SidebarServerResponse';
import GroupSyncSettings from '../ServiceCreateSteps/GroupSyncSettings';

const LdapActions = ActionsProvider.getActions('Ldap');

const ServiceCreateLDAP = () => {
  const [stepsState, setStepsState] = useState({
    activeStepKey: 'serverConfig',
    formValues: {
      serverConfig: {
        uriHost: 'localhost',
        uriPort: 389,
        useStartTLS: true,
        trustAllCertificates: false,
      },
      userSync: {},
    },
  });

  const { uriHost, uriPort, systemUsername, systemPassword } = stepsState.formValues.serverConfig;
  const { searchBaseDN, searchPattern, displayNameAttribute } = stepsState.formValues.userSync;
  const wizardFormValues = {};

  const isServerConfigValid = !!(uriHost && !!uriPort && systemUsername && systemPassword);
  const isUserSyncSettingValid = !!(searchBaseDN && searchPattern && displayNameAttribute);

  const _handleStepChange = (stepKey: $PropertyType<Step, 'key'>) => setStepsState({ ...stepsState, activeStepKey: stepKey });

  const _handleSubmitAll = () => {
    console.log('test', isServerConfigValid, isUserSyncSettingValid);

    if (isServerConfigValid && isUserSyncSettingValid) {
      const { serverConfig, userSync } = stepsState.formValues;
      // Temporary until we defined the correct request payload
      const ldapURI = `${new URI('').host(serverConfig.uriHost).port(serverConfig.uriPort).scheme('ldap')}`;
      const ldapSettings = {
        active_directory: false,
        additional_default_groups: [],
        default_group: 'Reader',
        display_name_attribute: userSync.displayNameAttribute,
        enabled: true,
        group_id_attribute: '',
        group_mapping: {},
        group_search_base: '',
        group_search_pattern: '',
        ldap_uri: ldapURI,
        search_base: serverConfig.searchBase,
        search_pattern: serverConfig.searchPattern,
        system_password_set: !!serverConfig.systemPassword,
        system_username: serverConfig.systemUsername,
        trust_all_certificates: serverConfig.trustAllCertificates,
        use_start_tls: serverConfig.useStartTLS,
      };

      LdapActions.update(ldapSettings).then((response) => {
        if (response) {
          history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
        }
      });
    }
  };

  const _handleFieldUpdate = (stepKey: $PropertyType<Step, 'key'>, event: SyntheticInputEvent<HTMLInputElement>, values: {[string]: mixed}) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;

    setStepsState({
      ...stepsState,
      formValues: {
        ...stepsState.formValues,
        [String(stepKey)]: {
          ...values,
          [event.target.name]: value,
        },
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
                             onChange={(event, values) => _handleFieldUpdate('serverConfig', event, values)} />
      ),

    },
    {
      key: 'userSync',
      title: 'User Synchronisation',
      component: (
        <UserSyncSettings onSubmit={_handleStepChange}
                          onSubmitAll={_handleSubmitAll}
                          onChange={(event, values) => _handleFieldUpdate('userSync', event, values)} />
      ),
      disabled: !isServerConfigValid,
    },
    {
      key: 'groupSync',
      title: 'Group Synchronisation',
      component: (
        <GroupSyncSettings onSubmit={_handleStepChange}
                           onSubmitAll={_handleSubmitAll}
                           onChange={(event, values) => _handleFieldUpdate('groupSync', event, values)}
                           wizardFormValues={wizardFormValues} />
      ),
      disabled: !isUserSyncSettingValid,
    },
  ];

  return (
    <ServiceStepsContext.Provider value={{ ...stepsState, setStepsState }}>
      <ServiceStepsContext.Consumer>
        {({ activeStepKey: activeStep }) => (
          <Wizard horizontal
                  justified
                  activeStep={activeStep}
                  onStepChange={_handleStepChange}
                  hidePreviousNextButtons
                  steps={wizardSteps}>
            <SidebarServerResponse />
          </Wizard>
        )}
      </ServiceStepsContext.Consumer>
    </ServiceStepsContext.Provider>
  );
};

export default ServiceCreateLDAP;
