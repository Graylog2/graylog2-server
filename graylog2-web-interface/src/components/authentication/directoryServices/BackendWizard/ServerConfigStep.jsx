// @flow strict

import * as React from 'react';
import styled from 'styled-components';
import { useContext } from 'react';
import { Formik, Form, Field } from 'formik';

import { formHasErrors, validateField } from 'util/FormsUtils';
import { FormikFormGroup, FormikInput, InputOptionalInfo as Opt } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';
import { Input } from 'components/bootstrap';

import BackendWizardContext from './BackendWizardContext';

export type StepKeyType = 'server-configuration';
export const STEP_KEY: StepKeyType = 'server-configuration';
export const FORM_VALIDATION = {
  serverHost: {
    required: true,
  },
  serverPort: {
    required: true,
    min: 1,
    max: 65535,
  },
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
  formRef: React.Ref<typeof Formik>,
  help: { [inputName: string]: ?React.Node },
  onSubmit: () => void,
  onSubmitAll: () => Promise<void>,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const ServerConfigStep = ({ formRef, help = {}, onSubmit, onSubmitAll, submitAllError, validateOnMount }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const { backendHasPassword } = stepsState.authBackendMeta;

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

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

  return (
    // $FlowFixMe innerRef works as expected
    <Formik initialValues={stepsState.formValues}
            innerRef={formRef}
            onSubmit={onSubmit}
            validateOnBlur={false}
            validateOnChange={false}
            validateOnMount={validateOnMount}>
      {({ isSubmitting, setFieldValue, values, validateForm }) => (
        <Form className="form form-horizontal">
          <Input id="uri-host"
                 label="Server Address"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <>
              <ServerUrl className="input-group">
                <FormikInput formGroupClassName=""
                             name="serverHost"
                             placeholder="Hostname"
                             validate={validateField(FORM_VALIDATION.serverPort)} />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput formGroupClassName=""
                             name="serverPort"
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
                           label={<>System User DN <Opt /></>}
                           name="systemUserDn"
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
                             placeholder="System Password"
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
