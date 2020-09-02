// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';

import { Select } from 'components/common';
import { Button } from 'components/graylog';

const AUTHENTICATION_PROVIDER_OPTIONS = [
  { label: 'LDAP', value: 'ldap' },
  { label: 'Active Directory', value: 'active-directory' },
];

const AuthenticationProviderSelect = () => {
  const onSubmit = () => {};

  return (
    <Formik onSubmit={onSubmit}
            initialValues={{ authProvider: 'ldap' }}>
      {({ isSubmitting, isValid }) => (
        <Form className="form-inline">
          <div className="form-group" style={{ width: 300 }}>
            <Field name="authProvider">
              {({ field: { name, value, onChange } }) => (
                <Select placeholder="Select input"
                        options={AUTHENTICATION_PROVIDER_OPTIONS}
                        matchProp="label"
                        onChange={(authProvider) => onChange({ target: { value: authProvider, name } })}
                        value={value}
                        clearable={false} />
              )}
            </Field>
          </div>
          &nbsp;
          <Button bsStyle="success" type="submit" disabled={isSubmitting || !isValid}>Get started</Button>
        </Form>
      )}
    </Formik>
  );
};

export default AuthenticationProviderSelect;
