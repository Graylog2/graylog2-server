/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useState, useContext } from 'react';
import { Formik, Form } from 'formik';
import { useTranslation } from 'react-i18next';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { FormikInput, Spinner } from 'components/common';
import { Button, Row, Col } from 'components/bootstrap';
import type { LoginTestResult } from 'hooks/useAuthentication';

import ConnectionErrors, { NotificationContainer } from './ConnectionErrors';
import type { WizardFormValues } from './BackendWizardContext';
import BackendWizardContext from './BackendWizardContext';

type Props = {
  prepareSubmitPayload: (fromValues: WizardFormValues | null | undefined) => WizardSubmitPayload;
};

const UserLoginTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const { t } = useTranslation('authentication');
  const initialLoginStatus = {
    loading: false,
    success: false,
    testFinished: false,
    result: undefined,
    message: undefined,
    errors: [],
  };
  const [{ loading, testFinished, success, message, errors, result }, setLoginStatus] = useState<
    LoginTestResult & {
      loading: boolean;
      testFinished: boolean;
      result: LoginTestResult['result'] | null | undefined;
    }
  >(initialLoginStatus);
  const hasErrors = errors && errors.length >= 1;

  const _handleLoginTest = ({ username, password }) => {
    setLoginStatus({ ...initialLoginStatus, loading: true });

    return AuthenticationDomain.testLogin({
      backend_configuration: prepareSubmitPayload(undefined),
      user_login: { username, password },
      backend_id: authBackendMeta.backendId,
    })
      .then((response) => {
        setLoginStatus({
          loading: false,
          testFinished: true,
          message: response.message,
          result: response.result,
          errors: response.errors,
          success: response.success,
        });
      })
      .catch((error) => {
        const requestErrors = [error?.message, error?.additional?.res?.text];
        setLoginStatus({
          loading: false,
          success: false,
          testFinished: true,
          result: undefined,
          message: undefined,
          errors: requestErrors,
        });
      });
  };

  return (
    <>
      <p>{t('login_test.description')}</p>
      <Formik onSubmit={_handleLoginTest} initialValues={{ password: '', username: '' }}>
        <Form className="form">
          <Row className="no-bm">
            <Col sm={6}>
              <FormikInput label={t('login_test.username')} name="username" id="user-login-username" required />
            </Col>
            <Col sm={6}>
              <FormikInput
                label={t('login_test.password')}
                name="password"
                type="password"
                id="user-login-password"
                required
              />
            </Col>
          </Row>
          <Button type="submit">
            {loading ? <Spinner delay={0} text={t('login_test.submit')} /> : t('login_test.submit')}
          </Button>
          {!hasErrors && testFinished && (
            <NotificationContainer bsStyle={success ? 'success' : 'danger'}>
              <b>
                {!result?.user_exists && t('login_test.user_does_not_exist')}
                {result?.user_exists && <>{result?.login_success ? message : t('login_test.login_failed')}</>}
              </b>
              {result?.user_exists && result?.user_details && (
                <div>
                  <br />
                  <table className="table">
                    <thead>
                      <tr>
                        <th>{t('login_test.user_attribute')}</th>
                        <th>{t('login_test.value')}</th>
                      </tr>
                    </thead>

                    <tbody>
                      {Object.entries(result?.user_details).map(([key, value]) => (
                        <tr key={key}>
                          <td>{String(key)}</td>
                          <td>{String(value)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </NotificationContainer>
          )}
          {hasErrors && <ConnectionErrors errors={errors} message={message} />}
        </Form>
      </Formik>
    </>
  );
};

export default UserLoginTest;
