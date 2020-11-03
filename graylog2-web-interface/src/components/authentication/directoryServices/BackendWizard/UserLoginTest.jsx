// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import { Formik, Form } from 'formik';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { FormikInput, Spinner } from 'components/common';
import { Button, Row, Col } from 'components/graylog';

import ConnectionErrors, { NotificationContainer } from './ConnectionErrors';
import BackendWizardContext from './BackendWizardContext';

type Props = {
  prepareSubmitPayload: () => WizardSubmitPayload,
};

const UserLoginTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const initialLoginStatus = { loading: false, success: false, testFinished: false, result: {}, message: undefined, errors: [] };
  const [{ loading, testFinished, success, message, errors, result }, setLoginStatus] = useState(initialLoginStatus);
  const hasErrors = (errors && errors.length >= 1);

  const _handleLoginTest = ({ username, password }) => {
    setLoginStatus({ ...initialLoginStatus, loading: true });

    return AuthenticationDomain.testLogin({
      backend_configuration: prepareSubmitPayload(),
      user_login: { username, password },
      backend_id: authBackendMeta.backendId,
    }).then((response) => {
      setLoginStatus({
        loading: false,
        testFinished: true,
        message: response.message,
        result: response.result,
        errors: response.errors,
        success: response.success,
      });
    }).catch((error) => {
      const requestErrors = [error?.message, error?.additional?.res?.text];
      setLoginStatus({ loading: false, success: false, testFinished: true, result: {}, message: undefined, errors: requestErrors });
    });
  };

  return (
    <>
      <p>
        Verify the settings by loading the entry for the given user name. If you omit the password, no authentication attempt will be made.
      </p>
      <Formik onSubmit={_handleLoginTest} initialValues={{ password: '', username: '' }}>
        <Form className="form">
          <Row className="no-bm">
            <Col sm={6}>
              <FormikInput label="Username"
                           name="username"
                           required />
            </Col>
            <Col sm={6}>
              <FormikInput label="Password"
                           name="password"
                           type="password"
                           required />
            </Col>
          </Row>
          <Button type="submit">
            {loading ? <Spinner delay={0} text="Test User Login" /> : 'Test User Login'}
          </Button>
          {(!hasErrors && testFinished) && (
            <NotificationContainer bsStyle={success ? 'success' : 'danger'}>
              <b>
                {!result.user_exists && 'User does not exist'}
                {result.user_exists && (
                  <>
                    {result.login_success ? message : 'Login failed'}
                  </>
                )}
              </b>
              {(result.user_exists && result.user_details) && (
                <div>
                  <br />
                  <table className="table">
                    <thead>
                      <tr>
                        <th>User Attribute</th>
                        <th>Value</th>
                      </tr>
                    </thead>

                    <tbody>
                      {Object.entries(result.user_details).map(([key, value]) => (
                        <tr key={key}>
                          <td>
                            {String(key)}
                          </td>
                          <td>
                            {String(value)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </NotificationContainer>
          )}
          {hasErrors && (
            <ConnectionErrors errors={errors} message={message} />
          )}
        </Form>
      </Formik>
    </>
  );
};

export default UserLoginTest;
