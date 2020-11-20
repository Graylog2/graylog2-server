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
import { Formik, Form, Field, FormikProps } from 'formik';

import { validateField, formHasErrors } from 'util/FormsUtils';
import { FormikFormGroup, FormikInput, InputOptionalInfo as Opt } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';
import { Input } from 'components/bootstrap';

import BackendWizardContext, { WizardFormValues } from './BackendWizardContext';

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

    :last-child {
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
  formRef: React.Ref<FormikProps<WizardFormValues>>,
  help: { [inputName: string]: React.ReactNode | null | undefined },
  onSubmit: () => void,
  onSubmitAll: () => Promise<void>,
  submitAllError: React.ReactNode | null | undefined,
  validateOnMount: boolean,
};

const ServerConfigStep = ({ formRef, help = {}, onSubmit, onSubmitAll, submitAllError, validateOnMount }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const { backendValidationErrors, authBackendMeta: { backendHasPassword } } = stepsState;

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
    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  return (
    <Formik initialValues={stepsState.formValues}
            ref={formRef}
            initialErrors={backendValidationErrors}
            onSubmit={onSubmit}
            validateOnBlur={false}
            validateOnChange={false}
            validateOnMount={validateOnMount}>
      {({ isSubmitting, setFieldValue, values, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup help={help.title}
                           label="Title"
                           name="title"
                           placeholder="Title" />

          <FormikFormGroup help={help.description}
                           label={<>Description <Opt /></>}
                           type="textarea"
                           name="description"
                           placeholder="Description" />

          <Input id="uri-host"
                 label="Server Address"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <>
              <ServerUrl className="input-group">
                <FormikInput formGroupClassName=""
                             name="serverHost"
                             error={backendValidationErrors?.serverHost}
                             placeholder="Hostname"
                             validate={validateField(FORM_VALIDATION.serverHost)} />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput formGroupClassName=""
                             name="serverPort"
                             error={backendValidationErrors?.serverPort}
                             placeholder="Port"
                             type="number"
                             validate={validateField(FORM_VALIDATION.serverPort)} />
              </ServerUrl>

              <ProtocolOptions>
                <Field name="transportSecurity">
                  {({ field: { name, onChange, onBlur, value } }) => (
                    <>
                      <Input defaultChecked={value === 'none'}
                             formGroupClassName=""
                             id={name}
                             label="None"
                             onBlur={onBlur}
                             onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                             type="radio"
                             value="none" />
                      <Input defaultChecked={value === 'tls'}
                             formGroupClassName=""
                             id={name}
                             label="TLS"
                             onBlur={onBlur}
                             onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                             type="radio"
                             value="tls" />
                      <Input defaultChecked={value === 'start_tls'}
                             formGroupClassName=""
                             id={name}
                             label="StartTLS"
                             onBlur={onBlur}
                             onChange={(e) => _onTransportSecurityChange(e, values, setFieldValue, onChange)}
                             type="radio"
                             value="start_tls" />
                    </>
                  )}
                </Field>

                <FormikInput formGroupClassName=""
                             label="Verify Certificates"
                             name="verifyCertificates"
                             type="checkbox" />
              </ProtocolOptions>

            </>
          </Input>
          <FormikFormGroup help={help.systemUserDn}
                           error={backendValidationErrors?.systemUserDn}
                           label={<>System User DN <Opt /></>}
                           name="systemUserDn"
                           validate={validateField(FORM_VALIDATION.systemUserDn)}
                           placeholder="System User DN" />

          {(backendHasPassword && values.systemUserPassword === undefined) ? (
            <Input id="systemPassword"
                   label={<>System Password <Opt /></>}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Button type="button" onClick={() => setFieldValue('systemUserPassword', '')}>
                Reset Password
              </Button>
            </Input>
          ) : (
            <FormikFormGroup autoComplete="authentication-service-password"
                             buttonAfter={(backendHasPassword && values.systemUserPassword !== undefined) ? (
                               <Button type="button" onClick={() => setFieldValue('systemUserPassword', undefined)}>
                                 Undo Reset
                               </Button>
                             ) : undefined}
                             help={help.systemUserPassword}
                             label={<>System Password <Opt /></>}
                             name="systemUserPassword"
                             error={backendValidationErrors?.systemUserPassword}
                             placeholder="System Password"
                             validate={validateField(FORM_VALIDATION.systemUserPassword)}
                             type="password" />
          )}

          {submitAllError}

          <ButtonToolbar className="pull-right">
            <Button disabled={isSubmitting}
                    onClick={() => _onSubmitAll(validateForm)}
                    type="button">
              Finish & Save Service
            </Button>
            <Button bsStyle="primary"
                    disabled={isSubmitting}
                    type="submit">
              Next: User Synchronization
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

export default ServerConfigStep;
