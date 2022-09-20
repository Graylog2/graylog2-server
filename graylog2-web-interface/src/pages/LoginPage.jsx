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
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled, { createGlobalStyle } from 'styled-components';
import { useQuery } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';

import { DocumentTitle, Icon } from 'components/common';
import { Alert, Button } from 'components/bootstrap';
import LoginForm from 'components/login/LoginForm';
import LoginBox from 'components/login/LoginBox';
import authStyles from 'theme/styles/authStyles';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AppConfig from 'util/AppConfig';
import { LOGIN_INITIALIZING_STATE, LOGIN_INITIALIZED_STATE } from 'logic/authentication/constants';
import { SessionActions } from 'stores/sessions/SessionStore';

import LoadingPage from './LoadingPage';

const LoginPageStyles = createGlobalStyle`
  ${authStyles}
`;

const StyledButton = styled(Button)`
  margin-top: 1em;
  display: inline-block;
  cursor: pointer;
`;

const StyledPre = styled.pre`
  white-space: pre-line;
`;

const useActiveBackend = (isCloud) => {
  const cloudBackendLoader = () => {
    if (isCloud) {
      return Promise.resolve('oidc-v1');
    }

    return AuthenticationDomain.loadActiveBackendType();
  };

  const { data, isSuccess } = useQuery(['activeBackendType'], cloudBackendLoader);

  return [data, isSuccess];
};

const ErrorFallback = ({ error, resetErrorBoundary }) => {
  const isCloud = AppConfig.isCloud();

  return (
    <Alert bsStyle="danger">
      {isCloud ? (
        <p>Error loading login screen, please contact your Graylog account manager.</p>
      ) : (
        <>
          <p>
            Error using active authentication service login. Please check its configuration or contact your
            Graylog account manager. Error details:
          </p>
          <StyledPre>{error.message}</StyledPre>
          <Button bsStyle="danger" onClick={resetErrorBoundary}>Login with default method</Button>
        </>
      )}
    </Alert>
  );
};

ErrorFallback.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
  }).isRequired,
  resetErrorBoundary: PropTypes.func.isRequired,
};

const LoginPage = () => {
  const [didValidateSession, setDidValidateSession] = useState(false);
  const [lastError, setLastError] = useState(undefined);
  const [useFallback, setUseFallback] = useState(false);
  const [enableExternalBackend, setEnableExternalBackend] = useState(true);
  const [loginFormState, setLoginFormState] = useState(LOGIN_INITIALIZING_STATE);
  const isCloud = AppConfig.isCloud();
  const [activeBackend, isBackendDetermined] = useActiveBackend(isCloud);

  const registeredLoginComponents = PluginStore.exports('loginProviderType');
  const loginComponent = registeredLoginComponents.find((c) => c.type === activeBackend);
  const hasCustomLogin = loginComponent && loginComponent.formComponent;

  useEffect(() => {
    const sessionPromise = SessionActions.validate().then((response) => {
      setDidValidateSession(true);

      return response;
    });

    return () => {
      sessionPromise.cancel();
    };
  }, []);

  useEffect(() => {
    setLastError(undefined);
  }, [useFallback]);

  const resetLastError = () => {
    setLastError(undefined);
  };

  const formatLastError = () => {
    if (lastError) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger">
            <button type="button" className="close" onClick={resetLastError}>&times;</button>{lastError}
          </Alert>
        </div>
      );
    }

    return null;
  };

  const renderLoginForm = () => {
    if (!useFallback && hasCustomLogin) {
      const { formComponent: PluginLoginForm } = loginComponent;

      return (
        <ErrorBoundary FallbackComponent={ErrorFallback}
                       onError={() => setEnableExternalBackend(false)}
                       onReset={() => setUseFallback(true)}>
          <PluginLoginForm onErrorChange={setLastError} setLoginFormState={setLoginFormState} />
        </ErrorBoundary>
      );
    }

    return <LoginForm onErrorChange={setLastError} />;
  };

  if (!didValidateSession || !isBackendDetermined) {
    return (
      <LoadingPage />
    );
  }

  const shouldDisplayFallbackLink = hasCustomLogin
  && enableExternalBackend
  && !isCloud
  && loginFormState === LOGIN_INITIALIZED_STATE;

  return (
    <DocumentTitle title="Sign in">
      <LoginBox>
        <legend><Icon name="users" /> Welcome to Graylog</legend>
        <LoginPageStyles />
        {formatLastError()}
        {renderLoginForm()}
        {shouldDisplayFallbackLink && (
        <StyledButton as="a" onClick={() => setUseFallback(!useFallback)}>
          {`Login with ${useFallback ? loginComponent.type.replace(/^\w/, (c) => c.toUpperCase()) : 'default method'}`}
        </StyledButton>
        )}
      </LoginBox>
    </DocumentTitle>
  );
};

export default LoginPage;
