/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import styled from 'styled-components';
import { useContext } from 'react';
import type { FormikProps } from 'formik';
import { Formik, Form, Field } from 'formik';
import { useTranslation } from 'react-i18next';

import { validateField, formHasErrors } from 'util/FormsUtils';
import { FormikFormGroup, FormikInput, InputOptionalInfo as Opt } from 'components/common';
import { Input, Button, ButtonToolbar } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { WizardFormValues } from './BackendWizardContext';
import BackendWizardContext from './BackendWizardContext';

export const STEP_KEY = 'server-configuration';
// Form validation needs to include all input names
// to be able to associate backend validation errors with the form
export const FORM_VALIDATION = {
  title: {
    required: true,
  },
  serverHost: {
    required: true,
  },
  serverPort: {
    required: true,
    min: 1,
    max: 65535,
  },
  description: {},
  transportSecurity: {},
  verifyCertificates: {},
  systemUserDn: {},
  systemUserPassword: {},
};

const ServerUrl = styled.div`
  display: flex;

  > * {
    align-self: flex-start;
    min-height: 34px;
    flex-grow: 1;

    &:last-child {
      flex: 0.8;
      min-width: 130px;
    }
  }

  .input-group-addon {
    display: flex;
    align-items: center;
    max-width: fit-content;
    min-width: fit-content;
  }
`;

const ProtocolOptions = styled.div`
  display: flex;

  div + * {
    margin-left: 10px;
  }
`;

type Props = {
  formRef: React.Ref<FormikProps<WizardFormValues>>;
  help?: { [inputName: string]: React.ReactElement | string | null | undefined };
  onSubmit: () => void;
  onSubmitAll: () => Promise<void>;
  submitAllError: React.ReactNode | null | undefined;
  validateOnMount: boolean;
};

const ServerConfigStep = ({ formRef, help = {}, onSubmit, onSubmitAll, submitAllError, validateOnMount }: Props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const {
    backendValidationErrors,
    authBackendMeta: { backendHasPassword },
  } = stepsState;
  const sendTelemetry = useSendTelemetry('directory-service');
  const { t } = useTranslation('authentication');

  const _onTransportSecurityChange = (event, values, setFieldValue, onChange) => {
    const currentValue = values.transportSecurity;
    const newValue = event.target.value;
    const defaultPort = 389;
    const defaultTlsPort = 636;

    if (currentValue === 'tls' && newValue !== 'tls' && values.serverPort === defaultTlsPort) {
      setFieldValue('serverPort', defaultPort);
    }

    if (currentValue !== 'tls' && newValue === 'tls' && values.serverPort === defaultPort) {
      setFieldValue('serverPort', defaultTlsPort);
    }

    onChange(event);
  };

  const _onSubmitAll = (validateForm) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.DIRECTORY_SERVER_CONFIG_SAVE_CLICKED, {
      app_action_value: 'server-configuration-save',
    });

    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  return (
    <Formik
      initialValues={stepsState.formValues}
      innerRef={formRef}
      initialErrors={backendValidationErrors}
      onSubmit={onSubmit}
      validateOnBlur={false}
      validateOnChange={false}
      validateOnMount={validateOnMount}>
      {({ isSubmitting, setFieldValue, values, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup
            help={help.title}
            label={t('server_config.title_label')}
            name="title"
            placeholder={t('server_config.title_label')}
          />

          <FormikFormGroup
            help={help.description}
            label={
              <>
                {t('server_config.description_label')} <Opt />
              </>
            }
            type="textarea"
            name="description"
            placeholder={t('server_config.description_label')}
          />

          <Input
            id="uri-host"
            label={t('server_config.server_address')}
            labelClassName="col-sm-3"
            wrapperClassName="col-sm-9">
            <>
              <ServerUrl className="input-group">
                <FormikInput
                  formGroupClassName=""
                  name="serverHost"
                  id="serverHost"
                  error={backendValidationErrors?.serverHost}
                  placeholder={t('server_config.hostname')}
                  validate={validateField(FORM_VALIDATION.serverHost)}
                />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput
                  formGroupClassName=""
                  name="serverPort"
                  id="serverPort"
                  error={backendValidationErrors?.serverPort}
                  placeholder={t('server_config.port')}
                  type="number"
                  validate={validateField(FORM_VALIDATION.serverPort)}
                />
              </ServerUrl>

              <ProtocolOptions>
                <Field name="transportSecurity">
                  {({ field: { name, onChange, onBlur, value } }) => (
                    <>
                      <Input
                        defaultChecked={value === 'none'}
                        formGroupClassName=""
                        id={name}
                        label={t('server_config.transport_security_none')}
                        onBlur={onBlur}
                        onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                        type="radio"
                        value="none"
                      />
                      <Input
                        defaultChecked={value === 'tls'}
                        formGroupClassName=""
                        id={name}
                        // eslint-disable-next-line graylog/no-untranslated-text -- protocol name
                        label="TLS"
                        onBlur={onBlur}
                        onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                        type="radio"
                        value="tls"
                      />
                      <Input
                        defaultChecked={value === 'start_tls'}
                        formGroupClassName=""
                        id={name}
                        // eslint-disable-next-line graylog/no-untranslated-text -- protocol name
                        label="StartTLS"
                        onBlur={onBlur}
                        onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                        type="radio"
                        value="start_tls"
                      />
                    </>
                  )}
                </Field>

                <FormikInput
                  formGroupClassName=""
                  label={t('server_config.verify_certificates')}
                  name="verifyCertificates"
                  id="verifyCertificates"
                  type="checkbox"
                />
              </ProtocolOptions>
            </>
          </Input>
          <FormikFormGroup
            help={help.systemUserDn}
            error={backendValidationErrors?.systemUserDn}
            label={t('server_config.system_user_dn')}
            name="systemUserDn"
            validate={validateField(FORM_VALIDATION.systemUserDn)}
            placeholder={t('server_config.system_user_dn')}
            required
          />

          {backendHasPassword && values.systemUserPassword === undefined ? (
            <Input
              id="systemPassword"
              label={
                <>
                  {t('server_config.system_password')} <Opt />
                </>
              }
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9">
              <Button type="button" onClick={() => setFieldValue('systemUserPassword', '')}>
                {t('server_config.reset_password')}
              </Button>
            </Input>
          ) : (
            <FormikFormGroup
              autoComplete="authentication-service-password"
              buttonAfter={
                backendHasPassword && values.systemUserPassword !== undefined ? (
                  <Button type="button" onClick={() => setFieldValue('systemUserPassword', undefined)}>
                    {t('server_config.undo_reset')}
                  </Button>
                ) : undefined
              }
              help={help.systemUserPassword}
              label={
                <>
                  {t('server_config.system_password')} <Opt />
                </>
              }
              name="systemUserPassword"
              error={backendValidationErrors?.systemUserPassword}
              placeholder={t('server_config.system_password')}
              validate={validateField(FORM_VALIDATION.systemUserPassword)}
              type="password"
            />
          )}

          {submitAllError}

          <ButtonToolbar className="pull-right">
            <Button disabled={isSubmitting} onClick={() => _onSubmitAll(validateForm)} type="button">
              {t('wizard.finish_and_save')}
            </Button>
            <Button
              bsStyle="primary"
              disabled={isSubmitting}
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.DIRECTORY_NEXT_USER_SYNC_CLICKED, {
                  app_action_value: 'usersync-button',
                });
              }}
              type="submit">
              {t('wizard.next_user_sync')}
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

export default ServerConfigStep;
