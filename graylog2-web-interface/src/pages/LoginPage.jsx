import React, { useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DocumentTitle, Icon } from 'components/common';
import { Alert } from 'components/graylog';
import LoginForm from 'components/login/LoginForm';
import LoginBox from 'components/login/LoginBox';

import AuthThemeStyles from 'theme/styles/authStyles';

const LoginPage = () => {
  const [lastError, setLastError] = useState(undefined);

  const _resetLastError = () => {
    setLastError(undefined);
  };

  const _formatLastError = () => {
    if (lastError) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger">
            <button type="button" className="close" onClick={_resetLastError}>&times;</button>{lastError}
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
        onErrorChange: setLastError,
      });
    }

    return <LoginForm onErrorChange={setLastError} />;
  };

  return (
    <DocumentTitle title="Sign in">
      <AuthThemeStyles />
      <LoginBox>
        <legend><Icon name="group" /> Welcome to Graylog</legend>
        {_formatLastError()}
        {renderLoginForm()}
      </LoginBox>
    </DocumentTitle>
  );
};

export default LoginPage;
