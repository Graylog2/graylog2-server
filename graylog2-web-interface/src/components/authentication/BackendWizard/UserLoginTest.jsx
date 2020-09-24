// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import { Formik, Form } from 'formik';
import styled from 'styled-components';

import type { LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { FormikInput, Spinner } from 'components/common';
import { Button, Row, Col, Alert } from 'components/graylog';

import BackendWizardContext from './contexts/BackendWizardContext';

const ResponseAlert = styled(Alert)`
  margin-top: 10px;
`;

type Props = {
  prepareSubmitPayload: () => LdapCreate,
};

const UserLoginTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const [{ loading, success, result }, setLoginStatus] = useState({ loading: false, success: false, result: undefined, message: undefined });

  const _handleLoginTest = ({ username, password }) => {
    setLoginStatus({ loading: true, error: undefined, success: false, result: undefined });

    const payload = { ...prepareSubmitPayload() };

    AuthenticationDomain.testLogin({
      backend_configuration: payload,
      user_login: { username, password },
      backend_id: authBackendMeta.backendId,
    }).then((response) => {
      if (response?.success) {
        setLoginStatus({ loading: false, message: response.message, result: response?.result, success: true });
      } else {
        setLoginStatus({ loading: false, message: response?.message, result: response?.result, success: false });
      }
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
                             name="username"
                             required />
              </Col>
              <Col sm={6}>
                <FormikInput label="Password"
                             name="password"
                             required />
              </Col>
            </Row>
            <Button type="submit" disabled={isSubmitting || !isValid}>
              {loading ? <Spinner delay={0} /> : 'Test Login'}
            </Button>
            {result && (
              <ResponseAlert bsStyle={success ? 'success' : 'error'}>
                <b>
                  {!result.user_exists && 'User does not exist'}
                  {result.user_exists && (
                    <>
                      {result.login_success ? 'Login was successful' : 'Login failed'}
                    </>
                  )}
                </b>
                {result.user_exists && (
                  <p>
                    {result.user_details && (
                      <>
                        <b>User attributes:</b><br />
                        {Object.entries(result.user_details).map(([key, value]) => <>{String(key)}: {String(value)}<br /></>)}
                      </>
                    )}
                  </p>
                )}
              </ResponseAlert>
            )}
          </Form>
        )}
      </Formik>
    </>
  );
};

export default UserLoginTest;
