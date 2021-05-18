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
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled, { createGlobalStyle } from 'styled-components';

import { DocumentTitle, Icon } from 'components/common';
import { Alert, Button } from 'components/graylog';
import LoginForm from 'components/login/LoginForm';
import LoginBox from 'components/login/LoginBox';
import authStyles from 'theme/styles/authStyles';
import CombinedProvider from 'injection/CombinedProvider';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AppConfig from 'util/AppConfig';

import LoadingPage from './LoadingPage';

const { SessionActions } = CombinedProvider.get('Session');

const LoginPageStyles = createGlobalStyle`
  ${authStyles}
`;

const StyledButton = styled(Button)`
  margin-top: 1em;
  display: inline-block;
  cursor: pointer;
`;

const LoginPage = () => {
  const [didValidateSession, setDidValidateSession] = useState(false);
  const [lastError, setLastError] = useState(undefined);
  const [activeBackend, setActiveBackend] = useState();
  const [isDetermined, setIsDetermined] = useState(false);
  const [useFallback, setUseFallback] = useState(false);

  const isCloud = AppConfig.isCloud();
  const registeredLoginComponents = PluginStore.exports('loginProviderType');
  const loginComponent = registeredLoginComponents.find((c) => c.type === activeBackend);
  const hasCustomLogin = loginComponent && loginComponent.formComponent;

  useEffect(() => {
    if (!isDetermined) {
      AuthenticationDomain.loadActiveBackendType()
        .then((backend) => {
          setIsDetermined(true);
          setActiveBackend(backend);
        });
    }
  }, [isDetermined]);

  useEffect(() => {
    const sessionPromise = SessionActions.validate().then((response) => {
      setDidValidateSession(true);

      return response;
    });

    return () => {
      sessionPromise.cancel();
    };
  }, []);

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
      return React.createElement(loginComponent.formComponent, {
        onErrorChange: setLastError,
      });
    }

    return <LoginForm onErrorChange={setLastError} />;
  };

  if (!didValidateSession || !isDetermined) {
    return (
      <LoadingPage />
    );
  }

  return (
    <DocumentTitle title="Sign in">
      <LoginBox>
        <legend><Icon name="users" /> Welcome to Graylog</legend>
        <LoginPageStyles />
        {formatLastError()}
        {renderLoginForm()}
        {hasCustomLogin && !isCloud && (
          <StyledButton as="a" onClick={() => setUseFallback(!useFallback)}>
            {`Login with ${useFallback ? loginComponent.type.replace(/^\w/, (c) => c.toUpperCase()) : 'default method'}`}
          </StyledButton>
        )}
      </LoginBox>
    </DocumentTitle>
  );
};

export default LoginPage;
