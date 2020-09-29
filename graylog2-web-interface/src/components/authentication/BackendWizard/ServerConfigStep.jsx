// @flow strict

import * as React from 'react';
import styled from 'styled-components';
import { useContext } from 'react';
import { Formik, Form, Field } from 'formik';

import { formHasErrors, validateField } from 'util/FormsUtils';
import { FormikFormGroup, FormikInput, InputOptionalInfo as Opt } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';
import { Input } from 'components/bootstrap';

import BackendWizardContext from './contexts/BackendWizardContext';

export type StepKeyType = 'server-configuration';
export const STEP_KEY: StepKeyType = 'server-configuration';
export const FORM_VALIDATION = {
  serverUrlHost: {
    required: true,
  },
  serverUrlPort: {
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

const defaultHelp = {
  systemUserDn: (
    <span>
      The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemUserPassword: 'The password for the initial connection to the Active Directory server.',
};

type Props = {
  formRef: React.Ref<typeof Formik>,
  help?: {
    systemUserDn?: React.Node,
    systemUserPassword?: React.Node,
  },
  onSubmit: () => void,
  onSubmitAll: () => void,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const ServerConfigStep = ({ formRef, help: propsHelp, onSubmit, onSubmitAll, submitAllError, validateOnMount }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const { backendHasPassword } = stepsState.authBackendMeta;
  const help = { ...defaultHelp, ...propsHelp };

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
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
                <span className="input-group-addon">
                  {stepsState.authBackendMeta.urlScheme}://
                </span>
                <FormikInput formGroupClassName=""
                             name="serverUrlHost"
                             placeholder="Hostname"
                             validate={validateField(FORM_VALIDATION.serverUrlPort)} />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput formGroupClassName=""
                             name="serverUrlPort"
                             placeholder="Port"
                             type="number"
                             validate={validateField(FORM_VALIDATION.serverUrlPort)} />
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
                             onChange={onChange}
                             type="radio"
                             value="none" />
                      <Input defaultChecked={value === 'tls'}
                             formGroupClassName=""
                             id={name}
                             label="TLS"
                             onBlur={onBlur}
                             onChange={onChange}
                             type="radio"
                             value="tls" />
                      <Input defaultChecked={value === 'start_tls'}
                             formGroupClassName=""
                             id={name}
                             label="StartTLS"
                             onBlur={onBlur}
                             onChange={onChange}
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

          {(backendHasPassword && values.systemUserPassword === undefined)
            ? (
              <Input id="systemPassword"
                     label={<>System Password <Opt /></>}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Button type="button" onClick={() => setFieldValue('systemUserPassword', '')}>
                  Reset Password
                </Button>
              </Input>
            )
            : (
              <FormikFormGroup autoComplete="authentication-service-password"
                               buttonAfter={(backendHasPassword && values.systemUserPassword !== undefined) && (
                               <Button type="button" onClick={() => setFieldValue('systemUserPassword', undefined)}>
                                 Undo Reset
                               </Button>
                               )}
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
              Next: User Synchronisation
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

ServerConfigStep.defaultProps = {
  help: {},
};

export default ServerConfigStep;
