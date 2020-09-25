// @flow strict
import * as React from 'react';
import { useState, useRef } from 'react';
import { compact } from 'lodash';
import PropTypes from 'prop-types';
import URI from 'urijs';

import Routes from 'routing/Routes';
import { validateField } from 'util/FormsUtils';
import history from 'util/History';
import type { LdapCreate } from 'logic/authentication/ldap/types';
import { Row, Col, Alert } from 'components/graylog';
import Wizard, { type Step } from 'components/common/Wizard';

import BackendWizardContext, { type WizardStepsState, type WizardFormValues, type AuthBackendMeta } from './contexts/BackendWizardContext';
import { FORM_VALIDATION as SERVER_CONFIG_VALIDATION, STEP_KEY as SERVER_CONFIG_KEY } from './ServerConfigStep';
import { FORM_VALIDATION as USER_SYNC_VALIDATION, STEP_KEY as USER_SYNC_KEY } from './UserSyncStep';
import { STEP_KEY as GROUP_SYNC_KEY } from './GroupSyncStep';
import wizardSteps from './wizardSteps';
import Sidebar from './Sidebar';

const FORMS_VALIDATION = {
  [SERVER_CONFIG_KEY]: SERVER_CONFIG_VALIDATION,
  [USER_SYNC_KEY]: USER_SYNC_VALIDATION,
};

const SubmitAllError = ({ error }: { error: string }) => (
  <Row>
    <Col xs={9} xsOffset={3}>
      <Alert bsStyle="danger">
        <b>Failed to create authentication service</b><br />
        {error}
      </Alert>
    </Col>
  </Row>
);

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
    backendId,
    serviceTitle,
    serviceType,
    urlScheme,
  } = stepsState.authBackendMeta;
  let password = { system_user_password: systemUserPassword };
  const serverUrl = `${new URI('').host(serverUrlHost).port(serverUrlPort).scheme(urlScheme)}`;

  // Only update password on edit if necessary, if a users resets the password it is null
  if (backendId && systemUserPassword === undefined) {
    password = {};
  }

  return {
    default_roles: defaultRoles.split(','),
    description: '',
    title: `${serviceTitle} ${serverUrl}`,
    config: {
      server_urls: [serverUrl],
      system_user_dn: systemUserDn,
      transport_security: transportSecurity,
      type: serviceType,
      user_full_name_attribute: userFullNameAttribute,
      user_name_attribute: userNameAttribute,
      user_search_base: userSearchBase,
      user_search_pattern: userSearchPattern,
      verify_certificates: verifyCertificates,
      ...password,
    },
  };
};

const _getInvalidStepKeys = (formValues) => {
  const invalidStepKeys = Object.entries(FORMS_VALIDATION).map(([stepKey, formValidation]) => {
    // $FlowFixMe formValidation is valid input for Object.entries
    const stepHasError = Object.entries(formValidation).some(([fieldName, fieldValidation]) => {
      return !!validateField(fieldValidation)(formValues?.[fieldName]);
    });

    return stepHasError ? stepKey : undefined;
  });

  // Remove undefined values
  return compact(invalidStepKeys);
};

const _onSubmitAll = (stepsState, setSubmitAllError, onSubmit, getUpdatedFormsValues, getSubmitPayload, validateSteps) => {
  const formValues = getUpdatedFormsValues();
  const invalidStepKeys = validateSteps(formValues);
  setSubmitAllError();

  if (invalidStepKeys.length >= 1) {
    return;
  }

  const payload = getSubmitPayload(formValues);

  onSubmit(payload).then(() => {
    history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE);
  }).catch((error) => {
    setSubmitAllError(error);
  });
};

type Props = {
  authBackendMeta: AuthBackendMeta,
  initialStepKey: $PropertyType<Step, 'key'>,
  initialValues: WizardFormValues,
  onSubmit: (LdapCreate) => Promise<void>,
};

const BackendWizard = ({ initialValues, initialStepKey, onSubmit, authBackendMeta }: Props) => {
  const [stepsState, setStepsState] = useState<WizardStepsState>({
    activeStepKey: initialStepKey,
    authBackendMeta,
    formValues: initialValues,
    invalidStepKeys: [],
  });
  const [submitAllError, setSubmitAllError] = useState<?string>();
  const formRefs = {
    [SERVER_CONFIG_KEY]: useRef(),
    [USER_SYNC_KEY]: useRef(),
    [GROUP_SYNC_KEY]: useRef(),
  };

  const _getUpdatedFormsValues = () => {
    const activeForm = formRefs[stepsState.activeStepKey]?.current;

    return { ...stepsState.formValues, ...activeForm?.values };
  };

  const _validateSteps = (formValues: WizardFormValues): Array<string> => {
    const invalidStepKeys = _getInvalidStepKeys(formValues);

    setStepsState({
      ...stepsState,
      activeStepKey: invalidStepKeys[0],
      formValues,
      invalidStepKeys,
    });

    return invalidStepKeys;
  };

  const _getSubmitPayload = _prepareSubmitPayload(stepsState, _getUpdatedFormsValues);

  const _setActiveStepKey = (stepKey: $PropertyType<Step, 'key'>) => {
    const formValues = _getUpdatedFormsValues();
    let invalidStepKeys = [...stepsState.invalidStepKeys];

    // Only update invalid steps keys, we create them on submit all only
    if (invalidStepKeys.length >= 1) {
      invalidStepKeys = _getInvalidStepKeys(formValues);
    }

    setStepsState({
      ...stepsState,
      invalidStepKeys,
      formValues,
      activeStepKey: stepKey,
    });
  };

  const _handleSubmitAll = () => _onSubmitAll(
    stepsState,
    setSubmitAllError,
    onSubmit,
    _getUpdatedFormsValues,
    _getSubmitPayload,
    _validateSteps,
  );

  const steps = wizardSteps({
    formRefs,
    handleSubmitAll: _handleSubmitAll,
    invalidStepKeys: stepsState.invalidStepKeys,
    setActiveStepKey: _setActiveStepKey,
    submitAllError: submitAllError && <SubmitAllError error={submitAllError} />,
  });

  return (
    <BackendWizardContext.Provider value={{ ...stepsState, setStepsState }}>
      <BackendWizardContext.Consumer>
        {({ activeStepKey: activeStep }) => (
          <Wizard activeStep={activeStep}
                  hidePreviousNextButtons
                  horizontal
                  justified
                  onStepChange={_setActiveStepKey}
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
    userFullNameAttribute: 'cn',
    userNameAttribute: 'uid',
    verifyCertificates: true,
  },
};

BackendWizard.propTypes = {
  authBackendMeta: PropTypes.shape({
    backendHasPassword: PropTypes.bool,
    backendId: PropTypes.string,
    serviceTitle: PropTypes.string.isRequired,
    serviceType: PropTypes.string.isRequired,
    urlScheme: PropTypes.string.isRequired,
  }).isRequired,
  initialStepKey: PropTypes.string,
  initialValues: PropTypes.object,
};

export default BackendWizard;
