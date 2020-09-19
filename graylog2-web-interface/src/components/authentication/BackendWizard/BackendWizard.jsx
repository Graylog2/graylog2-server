// @flow strict
import * as React from 'react';
import { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';
import { compact } from 'lodash';

import { validateField } from 'util/FormsUtils';
import Wizard, { type Step } from 'components/common/Wizard';
import Routes from 'routing/Routes';
import history from 'util/History';
import type { LdapCreate } from 'logic/authentication/ldap/types';

import wizardSteps from './wizardSteps';
import BackendWizardContext, { type WizardStepsState, type WizardFormValues, type AuthBackendMeta } from './contexts/BackendWizardContext';
import { FORM_VALIDATION as SERVER_CONFIG_VALIDATION, STEP_KEY as SERVER_CONFIG_KEY } from './ServerConfigStep';
import { FORM_VALIDATION as USER_SYNC_VALIDATION, STEP_KEY as USER_SYNC_KEY } from './UserSyncStep';
import { STEP_KEY as GROUP_SYNC_KEY } from './GroupSyncStep';
import Sidebar from './Sidebar';

const FORMS_VALIDATION = {
  [SERVER_CONFIG_KEY]: SERVER_CONFIG_VALIDATION,
  [USER_SYNC_KEY]: USER_SYNC_VALIDATION,
};

type Props = {
  initialValues: WizardFormValues,
  initialStepKey: $PropertyType<Step, 'key'>,
  onSubmit: (LdapCreate) => Promise<void>,
  authBackendMeta: AuthBackendMeta,
};

const _prepareSubmitPayload = (stepsState, getUpdatedFormsValues) => (overrideFormValues) => {
  // We need to ensure that we are using the actual form values
  // It is possible to provide the already updated form values, so we do not need to get them twice
  const formValues = overrideFormValues ?? getUpdatedFormsValues();
  const {
    defaultRoles = '',
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
  } = formValues;
  const {
    serviceType,
    serviceTitle,
    urlScheme,
  } = stepsState.authBackendMeta;
  const serverUrl = `${new URI('').host(serverUrlHost).port(serverUrlPort).scheme(urlScheme)}`;

  return {
    title: `${serviceTitle} - ${serverUrl}`,
    description: '',
    default_roles: defaultRoles.split(','),
    config: {
      type: serviceType,
      server_urls: [serverUrl],
      system_user_dn: systemUserDn,
      system_password: systemUserPassword,
      transport_security: transportSecurity,
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

const _onSubmitAll = (stepsState, setStepsState, onSubmit, getUpdatedFormsValues, getSubmitPayload) => {
  const formValues = getUpdatedFormsValues();
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

  const payload = getSubmitPayload(formValues);

  onSubmit(payload).then(() => {
    history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
  });
};

const BackendWizard = ({ initialValues, initialStepKey, onSubmit, authBackendMeta }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    authBackendMeta: authBackendMeta,
    activeStepKey: initialStepKey,
    formValues: initialValues,
    invalidStepKeys: [],
  });
  const formRefs = {
    [SERVER_CONFIG_KEY]: useRef(),
    [USER_SYNC_KEY]: useRef(),
    [GROUP_SYNC_KEY]: useRef(),
  };

  const _getUpdatedFormsValues = () => {
    const activeForm = formRefs[stepsState.activeStepKey]?.current;

    return { ...stepsState.formValues, ...activeForm?.values };
  };

  const _getSubmitPayload = _prepareSubmitPayload(stepsState, _getUpdatedFormsValues);

  const _setActiveStepKey = (stepKey: $PropertyType<Step, 'key'>) => {
    const formValues = _getUpdatedFormsValues();
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

  const _handleSubmitAll = () => _onSubmitAll(stepsState, setStepsState, onSubmit, _getUpdatedFormsValues, _getSubmitPayload);

  const steps = wizardSteps({
    formRefs,
    invalidStepKeys: stepsState.invalidStepKeys,
    handleSubmitAll: _handleSubmitAll,
    setActiveStepKey: _setActiveStepKey,
  });

  return (
    <BackendWizardContext.Provider value={{ ...stepsState, setStepsState }}>
      <BackendWizardContext.Consumer>
        {({ activeStepKey: activeStep }) => (
          <Wizard horizontal
                  justified
                  activeStep={activeStep}
                  onStepChange={_setActiveStepKey}
                  hidePreviousNextButtons
                  steps={steps}>
            <Sidebar prepareSubmitPayload={_getSubmitPayload} />
          </Wizard>
        )}
      </BackendWizardContext.Consumer>
    </BackendWizardContext.Provider>
  );
};

BackendWizard.defaultProps = {
  initialStepKey: SERVER_CONFIG_KEY,
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
  authBackendMeta: PropTypes.objectOf({
    backendId: PropTypes.string,
    serviceType: PropTypes.string.isRequired,
    serviceTitle: PropTypes.string.isRequired,
    urlScheme: PropTypes.string.isRequired,
  }).isRequired,
};

export default BackendWizard;
