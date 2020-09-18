// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';
import { compact } from 'lodash';

import { validateField } from 'util/FormsUtils';
import Wizard, { type Step } from 'components/common/Wizard';
import Routes from 'routing/Routes';
import history from 'util/History';
import type { LdapCreate } from 'logic/authentication/ldap/types';

import BackendWizardContext, { type WizardStepsState, type WizardFormValues } from './contexts/BackendWizardContext';
import ServerConfiguration, { FormValidation as ServerConfigValidation } from './ServerConfiguration';
import UserSyncSettings, { FormValidation as UserSyncValidation } from './UserSyncSettings';
import Sidebar from './Sidebar';
import GroupSyncSettings from './GroupSyncSettings';
import StepTitleWarning from './StepTitleWarning';

const FORMS_VALIDATION = {
  serverConfig: ServerConfigValidation,
  userSync: UserSyncValidation,
};

type Props = {
  initialValues: WizardFormValues,
  initialStepKey: $PropertyType<Step, 'key'>,
  onSubmit: (LdapCreate) => Promise<void>,
  editing: boolean,
  authServiceType: string,
};

export const prepareSubmitPayload = ({
  authServiceType,
  defaultRoles,
  serverUrlHost,
  serverUrlPort,
  systemUserDn,
  systemUserPassword,
  transportSecurity,
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
      transport_security: transportSecurity,
      type: authServiceType,
      user_full_name_attribute: userFullNameAttribute,
      user_name_attribute: userNameAttribute,
      user_search_base: userSearchBase,
      user_search_pattern: userSearchPattern,
      verify_certificates: verifyCertificates,
    },
  };
};

const _invalidStepKeys = (formValues) => {
  const invalidStepKeys = Object.entries(FORMS_VALIDATION).map(([stepKey, formValidation]) => {
    const stepHasError = Object.entries(formValidation).some(([fieldName, fieldValidation]) => {
      return !!validateField(fieldValidation)(formValues?.[fieldName]);
    });

    return stepHasError ? stepKey : undefined;
  });

  // Remove undefined values
  return compact(invalidStepKeys);
};

const _onSubmitAll = (stepsState, setStepsState, onSubmit, getCurrentFormValues) => {
  const formValues = getCurrentFormValues();
  const invalidStepKeys = _invalidStepKeys(formValues);

  if (invalidStepKeys.length >= 1) {
    setStepsState({
      ...stepsState,
      formValues,
      invalidStepKeys,
      activeStepKey: invalidStepKeys[0],
    });

    return;
  }

  setStepsState({ ...stepsState, formValues, invalidStepKeys: [] });

  const payload = prepareSubmitPayload(formValues);

  onSubmit(payload).then(() => {
    history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
  });
};

const BackendWizard = ({ authServiceType, initialValues, initialStepKey, onSubmit, editing }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    activeStepKey: initialStepKey,
    formValues: { ...initialValues, authServiceType },
    invalidStepKeys: [],
  });
  const formRefs = {
    serverConfig: useRef(),
    userSync: useRef(),
  };

  const _getCurrentFormValues = () => {
    const activeForm = formRefs[stepsState.activeStepKey]?.current;

    return { ...stepsState.formValues, ...activeForm?.values };
  };

  const _handleSubmitAll = () => _onSubmitAll(stepsState, setStepsState, onSubmit, _getCurrentFormValues);

  const _handleStepChange = (stepKey: $PropertyType<Step, 'key'>) => {
    const formValues = _getCurrentFormValues();
    let invalidStepKeys = [...stepsState.invalidStepKeys];

    // Only update invalid steps keys, we create them on submit all only
    if (invalidStepKeys.length >= 1) {
      invalidStepKeys = _invalidStepKeys(formValues);
    }

    setStepsState({
      ...stepsState,
      invalidStepKeys,
      formValues,
      activeStepKey: stepKey,
    });
  };

  const wizardSteps = Immutable.OrderedSet([
    {
      key: 'serverConfig',
      title: (
        <>
          <StepTitleWarning invalidStepKeys={stepsState.invalidStepKeys} stepKey="serverConfig" />
          Server Configuration
        </>),
      component: (
        <ServerConfiguration onSubmit={() => _handleStepChange('userSync')}
                             onSubmitAll={_handleSubmitAll}
                             validateOnMount={stepsState.invalidStepKeys.includes('serverConfig')}
                             formRef={formRefs.serverConfig}
                             editing={editing} />
      ),
    },
    {
      key: 'userSync',
      title: (
        <>
          <StepTitleWarning invalidStepKeys={stepsState.invalidStepKeys} stepKey="userSync" />
          User Synchronisation
        </>
      ),
      component: (
        <UserSyncSettings onSubmit={() => _handleStepChange('groupSync')}
                          validateOnMount={stepsState.invalidStepKeys.includes('userSync')}
                          formRef={formRefs.userSync}
                          onSubmitAll={_handleSubmitAll} />
      ),
    },
    {
      key: 'groupSync',
      title: (
        <>
          <StepTitleWarning invalidStepKeys={stepsState.invalidStepKeys} stepKey="groupSync" />
          Group Synchronisation
        </>
      ),
      component: (
        <GroupSyncSettings validateOnMount={stepsState.invalidStepKeys.includes('groupSync')}
                           onSubmitAll={_handleSubmitAll} />
      ),
    },
  ]);

  return (
    <BackendWizardContext.Provider value={{ ...stepsState, setStepsState }}>
      <BackendWizardContext.Consumer>
        {({ activeStepKey: activeStep }) => (
          <Wizard horizontal
                  justified
                  activeStep={activeStep}
                  onStepChange={_handleStepChange}
                  hidePreviousNextButtons
                  steps={wizardSteps.toJS()}>
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
    transportSecurity: 'tls',
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
