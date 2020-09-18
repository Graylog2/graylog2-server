// @flow strict

import * as React from 'react';
import styled from 'styled-components';
import { useContext } from 'react';
import { Formik, Form } from 'formik';

import { validation, validateField } from 'util/FormsUtils';
import { FormikFormGroup, FormikInput } from 'components/common';
import { Input } from 'components/bootstrap';
import { Button, ButtonToolbar } from 'components/graylog';

import BackendWizardContext from './contexts/BackendWizardContext';

export const FormValidation = {
  serverUrlHost: {
    required: true,
  },
  serverUrlPort: {
    required: true,
    min: 2,
    max: 65535,
  },
};

type Props = {
  help?: {
    systemUserDn?: React.Node,
    systemPasswordDn?: React.Node,
  },
  formRef: any,
  onSubmit: (nextStepKey: string) => void,
  onSubmitAll: () => void,
  editing: boolean,
  validateOnMount: boolean,
};

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
  systemPasswordDn: 'The password for the initial connection to the Active Directory server.',
};

const ServerConfiguration = ({ help: propsHelp, onSubmit, editing, onSubmitAll, formRef, validateOnMount }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const help = { ...defaultHelp, ...propsHelp };

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!validation.hasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  console.log('validateOnMount', validateOnMount);

  return (
    <Formik initialValues={stepsState?.formValues} onSubmit={() => onSubmit('userSync')} innerRef={formRef} validateOnMount={validateOnMount} validateOnBlur={false} validateOnChange={false}>
      {({ isSubmitting, validateForm }) => (
        <Form className="form form-horizontal">
          <Input id="uri-host"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 label="Server Address">
            <>
              <div className="input-group">
                <span className="input-group-addon">ldap://</span>
                <FormikInput {...FormValidation.serverUrlHost}
                             name="serverUrlHost"
                             placeholder="Hostname"
                             formGroupClassName=""
                             validate={validateField(FormValidation.serverUrlPort)} />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput validate={validateField(FormValidation.serverUrlPort)}
                             type="number"
                             name="serverUrlPort"
                             placeholder="Port"
                             formGroupClassName="" />
              </div>

              <ProtocolOptions>
                <FormikInput type="radio"
                             name="transportSecurity"
                             formGroupClassName=""
                             value=""
                             label="None" />

                <FormikInput type="radio"
                             name="transportSecurity"
                             formGroupClassName=""
                             value="tls"
                             label="TLS" />

                <FormikInput type="radio"
                             name="transportSecurity"
                             formGroupClassName=""
                             value="startTls"
                             label="StartTLS" />

                <FormikInput type="checkbox"
                             name="verifyCertificates"
                             formGroupClassName=""
                             label="Verify Certificates" />
              </ProtocolOptions>

            </>
          </Input>
          <FormikFormGroup label="System User DN"
                           name="systemUserDn"
                           placeholder="System User DN"
                           help={help.systemUserDn} />

          <FormikFormGroup label="System Password"
                           name="systemPasswordDn"
                           autoComplete="autentication-service-password"
                           placeholder="System Password"
                           type="password"
                           help={help.systemPasswordDn} />
          <ButtonToolbar className="pull-right">
            {editing && (
              <Button type="button"
                      onClick={() => _onSubmitAll(validateForm)}
                      disabled={isSubmitting}>
                Finish & Save Identity Provider
              </Button>
            )}
            <Button bsStyle="primary"
                    type="submit"
                    disabled={isSubmitting}>
              Next: User Synchronisation
            </Button>
          </ButtonToolbar>

        </Form>
      )}
    </Formik>
  );
};

ServerConfiguration.defaultProps = {
  help: {},
};

export default ServerConfiguration;
