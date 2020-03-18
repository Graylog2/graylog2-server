import React, { useEffect, useState } from 'react';
import { connect } from 'reflux';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DocumentTitle, Icon } from 'components/common';
import { Alert } from 'components/graylog';
import LoginForm from 'components/login/LoginForm';
import LoginBox from 'components/login/LoginBox';

import AuthThemeStyles from 'theme/styles/authStyles';

import CombinedProvider from 'injection/CombinedProvider';
import LoadingPage from './LoadingPage';

const { SessionStore, SessionActions } = CombinedProvider.get('Session');

const LoginPage = () => {
  const [didValidateSession, setDidValidateSession] = useState(false);
  const [lastError, setLastError] = useState(false);

  useEffect(() => {
    const sessionPromise = SessionActions.validate().then((response) => {
      setDidValidateSession(true);
      return response;
    });

    return () => {
      sessionPromise.cancel();
    };
  }, []);

  const handleErrorChange = (nextError) => {
    setLastError(nextError);
  };

  const resetLastError = () => {
    handleErrorChange(undefined);
  };

  const formatLastError = (error) => {
    if (error) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger">
            <button type="button" className="close" onClick={resetLastError}>&times;</button>{error}
          </Alert>
        </div>
      );
    }
    return null;
  };

  const renderLoginForm = () => {
    const loginComponent = PluginStore.exports('loginProviderType');

    if (loginComponent.length === 1) {
      return React.createElement(loginComponent[0].formComponent, {
        onErrorChange: handleErrorChange,
      });
    }

    return <LoginForm onErrorChange={handleErrorChange} />;
  };

  if (!didValidateSession) {
    return (
      <LoadingPage />
    );
  }

  const alert = formatLastError(lastError);
  return (
    <DocumentTitle title="Sign in">
      <AuthThemeStyles />
      <LoginBox>
        <legend><Icon name="group" /> Welcome to Graylog</legend>
        {alert}
        {renderLoginForm()}
      </LoginBox>
    </DocumentTitle>
  );
};

export default connect(LoginPage, {
  sessionId: SessionStore,
}, ({
  sessionId: { sessionId } = '',
}) => ({ sessionId }));
