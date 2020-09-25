// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import { Formik, Form } from 'formik';

import type { LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { FormikInput, Spinner } from 'components/common';
import { Button, Row, Col } from 'components/graylog';

import ConnectionErrors, { NotificationContainer } from './ConnectionErrors';
import BackendWizardContext from './contexts/BackendWizardContext';

type Props = {
  prepareSubmitPayload: () => LdapCreate,
};

const UserLoginTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const initialLoginStatus = { loading: false, success: false, testFinished: false, result: {}, message: undefined, errors: [] };
  const [{ loading, testFinished, success, message, errors, result }, setLoginStatus] = useState(initialLoginStatus);
  const hasErrors = (errors && errors.length >= 1);

  const _handleLoginTest = ({ username, password }) => {
    setLoginStatus({ ...initialLoginStatus, loading: true });

    const payload = { ...prepareSubmitPayload() };

    return AuthenticationDomain.testLogin({
      backend_configuration: payload,
      user_login: { username, password },
      backend_id: authBackendMeta.backendId,
    }).then((response) => {
      if (response) {
        setLoginStatus({
          loading: false,
          testFinished: true,
          message: response.message,
          result: response.result,
          errors: response.errors,
          success: response.success,
        });
      }
    });
  };

  return (
    <>
      <p>
        Verify the settings by loading the entry for the given user name. If you omit the password, no authentication attempt will be made.
      </p>
      <Formik onSubmit={_handleLoginTest} initialValues={{ password: '', username: '' }}>
        <Form className="form">
          <Row>
            <Col sm={6}>
              <FormikInput label="Username"
                           name="username"
                           required />
            </Col>
            <Col sm={6}>
              <FormikInput label="Password"
                           name="password"
                           required />
            </Col>
          </Row>
          <Button type="submit">
            {loading ? <Spinner delay={0} /> : 'Test Login'}
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
            <p>
              <br />
              <table className="table">
                <tr>
                  <th>User Attribute</th>
                  <th>Value</th>
                </tr>

                {Object.entries(result.user_details).map(([key, value]) => {
                  return (
                    <tr>
                      <td>
                        {String(key)}
                      </td>
                      <td>
                        {String(value)}
                      </td>
                    </tr>
                  );
                })}
              </table>
            </p>
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
