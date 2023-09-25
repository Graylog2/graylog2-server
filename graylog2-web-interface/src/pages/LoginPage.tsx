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
import { useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { useQuery } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';

import LoginChrome from 'components/login/LoginChrome';
import DocumentTitle from 'components/common/DocumentTitle';
import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import LoginForm from 'components/login/LoginForm';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AppConfig from 'util/AppConfig';
import { LOGIN_INITIALIZING_STATE, LOGIN_INITIALIZED_STATE } from 'logic/authentication/constants';
import { SessionActions } from 'stores/sessions/SessionStore';
import usePluginEntities from 'hooks/usePluginEntities';

import LoadingPage from './LoadingPage';

const StyledButton = styled(Button)`
  margin-top: 1em;
  display: inline-block;
  cursor: pointer;
`;

const StyledPre = styled.pre`
  white-space: pre-line;
`;

const useActiveBackend = (isCloud: boolean) => {
  const cloudBackendLoader = () => {
    if (isCloud) {
      return Promise.resolve('oidc-v1');
    }

    return AuthenticationDomain.loadActiveBackendType();
  };

  const { data, isSuccess } = useQuery(['activeBackendType'], cloudBackendLoader);

  return [data, isSuccess];
};

type ErrorFallbackProps = {
  error: Error;
  resetErrorBoundary: (...args: Array<unknown>) => void;
};

const ErrorFallback = ({ error, resetErrorBoundary }: ErrorFallbackProps) => {
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

const useValidateSession = () => {
  const [didValidateSession, setDidValidateSession] = useState(false);

  useEffect(() => {
    const sessionPromise = SessionActions.validate().then((response) => {
      setDidValidateSession(true);

      return response;
    });

    return () => {
      sessionPromise.cancel();
    };
  }, []);

  return didValidateSession;
};

const LoginPage = () => {
  const didValidateSession = useValidateSession();
  const [lastError, setLastError] = useState<string | undefined>(undefined);
  const [useFallback, setUseFallback] = useState(false);
  const [enableExternalBackend, setEnableExternalBackend] = useState(true);
  const [loginFormState, setLoginFormState] = useState(LOGIN_INITIALIZING_STATE);
  const isCloud = AppConfig.isCloud();
  const [activeBackend, isBackendDetermined] = useActiveBackend(isCloud);

  const registeredLoginComponents = usePluginEntities('loginProviderType');
  const loginComponent = useMemo(() => registeredLoginComponents.find((c) => c.type === activeBackend), [activeBackend, registeredLoginComponents]);
  const CustomLogin = loginComponent?.formComponent;
  const hasCustomLogin = CustomLogin !== undefined;

  useEffect(() => {
    setLastError(undefined);
  }, [useFallback]);

  const resetLastError = useCallback(() => {
    setLastError(undefined);
  }, []);

  const PluggableLoginForm = useCallback(() => {
    if (!useFallback && CustomLogin) {
      return (
        <ErrorBoundary FallbackComponent={ErrorFallback}
                       onError={() => setEnableExternalBackend(false)}
                       onReset={() => setUseFallback(true)}>
          <CustomLogin onErrorChange={setLastError} setLoginFormState={setLoginFormState} />
        </ErrorBoundary>
      );
    }

    return <LoginForm onErrorChange={setLastError} />;
  }, [CustomLogin, useFallback]);

  const LastError = useCallback(() => {
    if (lastError) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger" onDismiss={resetLastError}>
            {lastError}
          </Alert>
        </div>
      );
    }

    return null;
  }, [lastError, resetLastError]);

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
    (
      <DocumentTitle title="Sign in">
        <LoginChrome>
          <LastError />
          <PluggableLoginForm />
          {shouldDisplayFallbackLink && (
          <StyledButton as="a" onClick={() => setUseFallback(!useFallback)}>
            {`Login with ${useFallback ? loginComponent.type.replace(/^\w/, (c) => c.toUpperCase()) : 'default method'}`}
          </StyledButton>
          )}
        </LoginChrome>
      </DocumentTitle>
    )
  );
};

export default LoginPage;
