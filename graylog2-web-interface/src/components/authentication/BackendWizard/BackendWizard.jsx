// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';

import Wizard, { type Step } from 'components/common/Wizard';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
import history from 'util/History';

import BackendWizardContext, { type WizardStepsState, type WizardFormValues } from './contexts/BackendWizardContext';
import ServerConfiguration from './ServerConfiguration';
import UserSyncSettings from './UserSyncSettings';
import Sidebar from './Sidebar';
import GroupSyncSettings from './GroupSyncSettings';

const LdapActions = ActionsProvider.getActions('Ldap');

type Props = {
  initialValues: WizardFormValues,
  initialStep: $PropertyType<Step, 'key'>,
  onSubmitAll: (WizardFormValues) => Promise<void>,
  editing?: boolean,
};

const BackendWizard = ({ initialValues, initialStep, onSubmitAll, editing }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    activeStepKey: initialStep,
    formValues: initialValues,
  });

  const { serverUriHost, serverUriPort, systemUsername, systemPassword, userSearchBase, userSearchPattern, displayNameAttribute, trustAllCertificates, useStartTLS } = stepsState.formValues;

  const isServerConfigValid = !!(serverUriHost && !!serverUriPort && systemUsername && systemPassword);
  const isUserSyncSettingValid = !!(userSearchBase && userSearchPattern && displayNameAttribute);

  const _handleStepChange = (stepKey: $PropertyType<Step, 'key'>) => setStepsState({ ...stepsState, activeStepKey: stepKey });

  const _handleSubmitAll = () => {
    if (isServerConfigValid && isUserSyncSettingValid) {
      // Temporary until we defined the correct request payload
      const ldapURI = `${new URI('').host(serverUriHost).port(serverUriPort).scheme('ldap')}`;
      const ldapSettings = {
        active_directory: false,
        additional_default_groups: [],
        default_group: 'Reader',
        display_name_attribute: displayNameAttribute,
        enabled: true,
        group_id_attribute: '',
        group_mapping: {},
        group_search_base: '',
        group_search_pattern: '',
        ldap_uri: ldapURI,
        search_base: userSearchBase,
        search_pattern: userSearchPattern,
        system_password_set: !!systemPassword,
        system_username: systemUsername,
        trust_all_certificates: trustAllCertificates,
        use_start_tls: useStartTLS,
      };

      LdapActions.update(ldapSettings).then((response) => {
        if (response) {
          history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
        }
      });

      onSubmitAll({});
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
    useStartTLS: true,
    trustAllCertificates: false,
  },
};

BackendWizard.propTypes = {
  initialStep: PropTypes.string,
  initialValues: PropTypes.object,
  editing: PropTypes.bool,
};

export default BackendWizard;
