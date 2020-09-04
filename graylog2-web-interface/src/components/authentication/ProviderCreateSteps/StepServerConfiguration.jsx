// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { FormikFormGroup, FormikField } from 'components/common';
import { Input } from 'components/bootstrap';

type Props = {
  initialValues?: {
    uriHost?: string,
    uriPort?: string,
    sslCheckbox?: boolean,
    systemUsername?: string,
  },
  help?: {
    systemUsername?: React.Node,
    systemPassword?: React.Node,
  },
};

const defaultHelp = {
  systemUsername: (
    <span>
      The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.<br />
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemPassword: 'The password for the initial connection to the Active Directory server.',
};

const StepServerConfiguration = ({ initialValues, help: propsHelp }: Props) => {
  const help = { ...defaultHelp, ...propsHelp };

  return (
    <Formik initialValues={initialValues} onSubmit={() => {}}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <Input id="uri-host"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 label="Server Address">
            <>
              <div className="input-group">
                <span className="input-group-addon">ldap://</span>
                <FormikField name="uriHost"
                             placeholder="Hostname"
                             required />
                <span className="input-group-addon input-group-separator">:</span>
                <FormikField type="number"
                             name="uriPort"
                             min="1"
                             max="65535"
                             placeholder="Port"
                             required />
              </div>
              <label className="checkbox-inline" htmlFor="useSSL">
                {/* checked={ldapUri.scheme() === 'ldaps'} ? */}
                <FormikField type="checkbox"
                             name="useSSL"
                             label="SSL" />
              </label>
              <label className="checkbox-inline" htmlFor="ldap-uri-starttls">
                <FormikField type="checkbox"
                             name="useStartTLS"
                             label="StartTLS" />
              </label>
              <label className="checkbox-inline" htmlFor="trustAllCertificates">
                <FormikField type="checkbox"
                             name="trustAllCertificates"
                             label="Allow untrusted certificates" />
              </label>
            </>
          </Input>
          <FormikFormGroup label="System Username"
                           name="systemUsername"
                           placeholder="System User DN"
                           required
                           help={help.systemUsername} />

          <FormikFormGroup label="System Password"
                           name="systemPassword"
                           placeholder="System Password"
                           required
                           help={help.systemPassword} />
        </Form>
      )}
    </Formik>
  );
};

StepServerConfiguration.defaultProps = {
  help: {},
  initialValues: {
    uriHost: 'localhost',
    uriPort: 389,
    useStartTLS: true,
    trustAllCertificates: false,
  },
};

export default StepServerConfiguration;
