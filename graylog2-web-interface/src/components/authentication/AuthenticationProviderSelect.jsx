// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';

import { availableProvidersOptions } from 'logic/authentication/availableProviders';
import history from 'util/History';
import Routes from 'routing/Routes';
import { Select } from 'components/common';
import { Button } from 'components/graylog';

const AuthenticationProviderSelect = () => {
  const onSubmit = ({ authProvider }) => {
    console.log(Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE);
    // history.push(Routes.SYSTEM.AUTHENTICATION.CREATE);
    history.push({ pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE, search: `?type=${authProvider}` });
  };

  return (
    <Formik onSubmit={onSubmit}
            initialValues={{ authProvider: 'ldap' }}>
      {({ isSubmitting, isValid }) => (
        <Form className="form-inline">
          <div className="form-group" style={{ width: 300 }}>
            <Field name="authProvider">
              {({ field: { name, value, onChange } }) => (
                <Select placeholder="Select input"
                        options={availableProvidersOptions}
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
