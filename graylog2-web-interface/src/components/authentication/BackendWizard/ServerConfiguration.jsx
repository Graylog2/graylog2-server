// @flow strict

import * as React from 'react';
import styled from 'styled-components';
import { useContext } from 'react';
import { Formik, Form } from 'formik';

import FormUtils from 'util/FormsUtils';
import { FormikFormGroup, FormikInput } from 'components/common';
import { Input } from 'components/bootstrap';
import { Button, ButtonToolbar } from 'components/graylog';

import BackendWizardContext from './contexts/BackendWizardContext';

type Props = {
  disableSubmitAll: boolean,
  help?: {
    systemUsername?: React.Node,
    systemPassword?: React.Node,
  },
  onSubmit: (nextStepKey: string) => void,
  onSubmitAll: () => void,
  onChange: (event: SyntheticInputEvent<HTMLInputElement>) => void,
  editing: boolean,
};

const ProtocolOptions = styled.div`
  display: flex;

  div + * {
    margin-left: 10px;
  }
`;

const defaultHelp = {
  systemUsername: (
    <span>
      The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemPassword: 'The password for the initial connection to the Active Directory server.',
};

const ServerConfiguration = ({ help: propsHelp, onChange, onSubmit, editing, onSubmitAll, disableSubmitAll }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const help = { ...defaultHelp, ...propsHelp };

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!FormUtils.validate.hasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  return (
    <Formik initialValues={stepsState?.formValues} onSubmit={() => onSubmit('userSync')}>
      {({ isSubmitting, isValid, validateForm }) => (
        <Form onChange={(event) => onChange(event)} className="form form-horizontal">
          <Input id="uri-host"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 label="Server Address">
            <>
              <div className="input-group">
                <span className="input-group-addon">ldap://</span>
                <FormikInput name="serverUriHost"
                             placeholder="Hostname"
                             formGroupClassName=""
                             validate={FormUtils.validation.isRequired('server host')}
                             required />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikInput type="number"
                             name="serverUriPort"
                             validate={FormUtils.validation.isRequired('server port')}
                             min="1"
                             max="65535"
                             placeholder="Port"
                             formGroupClassName=""
                             required />
              </div>

              {/* checked={ldapUri.scheme() === 'ldaps'} ? */}
              <ProtocolOptions>
                <FormikInput type="checkbox"
                             name="useSSL"
                             formGroupClassName=""
                             label="SSL" />

                <FormikInput type="checkbox"
                             name="useStartTls"
                             formGroupClassName=""
                             label="StartTLS" />

                <FormikInput type="checkbox"
                             name="trustAllCertificates"
                             formGroupClassName=""
                             label="Allow untrusted certificates" />

              </ProtocolOptions>

            </>
          </Input>
          <FormikFormGroup label="System Username"
                           name="systemUsername"
                           placeholder="System User DN"
                           required
                           validate={FormUtils.validation.isRequired('System Username')}
                           help={help.systemUsername} />

          <FormikFormGroup label="System Password"
                           name="systemPassword"
                           placeholder="System Password"
                           type="password"
                           help={help.systemPassword} />
          <ButtonToolbar className="pull-right">
            {editing && (
              <Button type="button"
                      onClick={() => _onSubmitAll(validateForm)}
                      disabled={!isValid || isSubmitting || disableSubmitAll}>
                Finish & Save Identity Provider
              </Button>
            )}
            <Button bsStyle="primary"
                    type="submit"
                    disabled={!isValid || isSubmitting}>
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
