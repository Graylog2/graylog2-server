// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import { Formik, Form } from 'formik';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { FormikInput, Spinner } from 'components/common';
import { Button, Row, Col, Panel } from 'components/graylog';

import BackendWizardContext from './contexts/BackendWizardContext';

const UserLoginTest = () => {
  const { formValues, prepareSubmitPayload } = useContext(BackendWizardContext);
  const [{ loading, success, result }, setLoginStatus] = useState({ loading: false, success: false, result: undefined });

  const _handleLoginTest = ({ username, password }) => {
    setLoginStatus({ loading: true, error: undefined, success: false, result: undefined });

    const payload = prepareSubmitPayload(formValues);

    AuthenticationDomain.testLogin({ backend_configuration: payload, backend_id: null, username, password }).then((response) => {
      setLoginStatus({ loading: false, success: response?.success, result: response?.result });
    });
  };

  return (
    <>
      <p>
        Verify the settings by loading the entry for the given user name. If you omit the password, no authentication attempt will be made.
      </p>
      <Formik onSubmit={_handleLoginTest} initialValues={{ password: '', username: '' }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form">
            <Row>
              <Col sm={6}>
                <FormikInput label="Username"
                             name="username" />
              </Col>
              <Col sm={6}>
                <FormikInput label="Password"
                             name="password" />
              </Col>
            </Row>
            <Button type="submit" disabled={isSubmitting || !isValid}>
              {loading ? <Spinner delay={0} /> : 'Test Login'}
            </Button>
            {result && (
              <Panel bsStyle={success ? 'success' : 'error'}>
                {!result.user_exists && <p>User does not exist</p>}
                {result.user_exists && (
                  <p>
                    {result.login_success ? 'Login was successful' : 'Login failed'}
                    {result.user_details && (
                      <>
                        <b>User attributes:</b><br />
                        {Object.entries(result.user_details).map(([key, value]) => <>{String(key)}: {String(value)}<br /></>)}
                      </>
                    )}
                  </p>
                )}
              </Panel>
            )}
          </Form>
        )}
      </Formik>
    </>
  );
};

export default UserLoginTest;
