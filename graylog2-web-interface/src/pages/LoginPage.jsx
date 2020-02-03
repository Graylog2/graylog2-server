import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DocumentTitle, Icon } from 'components/common';
import { Alert } from 'components/graylog';
import LoginForm from 'components/login/LoginForm';
import LoginBox from 'components/login/LoginBox';

import AuthThemeStyles from 'theme/styles/authStyles';

import CombinedProvider from 'injection/CombinedProvider';
import LoadingPage from './LoadingPage';

// eslint-disable-next-line import/no-webpack-loader-syntax
import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';

const { SessionStore, SessionActions } = CombinedProvider.get('Session');

const LoginPage = createReactClass({
  displayName: 'LoginPage',
  mixins: [Reflux.connect(SessionStore), Reflux.ListenerMethods],

  getInitialState() {
    return {
      didValidateSession: false,
    };
  },

  componentDidMount() {
    disconnectedStyle.use();
    SessionActions.validate().then((response) => {
      this.setState({ didValidateSession: true });
      return response;
    });
  },

  componentWillUnmount() {
    disconnectedStyle.unuse();
  },

  handleErrorChange(nextError) {
    this.setState({ lastError: nextError });
  },

  resetLastError() {
    this.handleErrorChange(undefined);
  },

  formatLastError(error) {
    if (error) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger">
            <button type="button" className="close" onClick={this.resetLastError}>&times;</button>{error}
          </Alert>
        </div>
      );
    }
    return null;
  },

  renderLoginForm() {
    const loginComponent = PluginStore.exports('loginProviderType');

    if (loginComponent.length === 1) {
      return React.createElement(loginComponent[0].formComponent, {
        onErrorChange: this.handleErrorChange,
      });
    }

    return <LoginForm onErrorChange={this.handleErrorChange} />;
  },

  render() {
    const { lastError, didValidateSession } = this.state;

    if (!didValidateSession) {
      return (
        <LoadingPage />
      );
    }

    const alert = this.formatLastError(lastError);
    return (
      <DocumentTitle title="Sign in">
        <AuthThemeStyles />
        <LoginBox>
          <legend><Icon name="group" /> Welcome to Graylog</legend>
          {alert}

          {this.renderLoginForm()}
        </LoginBox>
      </DocumentTitle>
    );
  },
});

export default LoginPage;
