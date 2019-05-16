import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Button, FormGroup, Alert } from 'react-bootstrap';
import { DocumentTitle } from 'components/common';

import { Input } from 'components/bootstrap';
import LoadingPage from './LoadingPage';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

// eslint-disable-next-line import/no-webpack-loader-syntax
import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';
// eslint-disable-next-line import/no-webpack-loader-syntax
import authStyle from '!style/useable!css!less!stylesheets/auth.less';

const SessionStore = StoreProvider.getStore('Session');
const SessionActions = ActionsProvider.getActions('Session');

const LoginPage = createReactClass({
  displayName: 'LoginPage',
  mixins: [Reflux.connect(SessionStore), Reflux.ListenerMethods],

  getInitialState() {
    return {
      loading: false,
    };
  },

  componentDidMount() {
    disconnectedStyle.use();
    authStyle.use();
    SessionActions.validate();
  },

  componentWillUnmount() {
    disconnectedStyle.unuse();
    authStyle.unuse();
    if (this.promise) {
      this.promise.cancel();
    }
  },

  onSignInClicked(event) {
    event.preventDefault();
    this.resetLastError();
    this.setState({ loading: true });
    const username = this.username.getValue();
    const password = this.password.getValue();
    const location = document.location.host;
    this.promise = SessionActions.login.triggerPromise(username, password, location);
    this.promise.catch((error) => {
      if (error.additional.status === 401) {
        this.setState({ lastError: 'Invalid credentials, please verify them and retry.' });
      } else {
        this.setState({ lastError: `Error - the server returned: ${error.additional.status} - ${error.message}` });
      }
    });
    this.promise.finally(() => {
      if (!this.promise.isCancelled()) {
        this.setState({ loading: false });
      }
    });
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

  resetLastError() {
    this.setState({ lastError: undefined });
  },

  render() {
    const { lastError, loading, validatingSession } = this.state;

    if (validatingSession) {
      return (
        <LoadingPage />
      );
    }

    const alert = this.formatLastError(lastError);
    return (
      <DocumentTitle title="Sign in">
        <div>
          <div className="container" id="login-box">
            <Row>
              <form className="col-md-4 col-md-offset-4 well" id="login-box-content" onSubmit={this.onSignInClicked}>
                <legend><i className="fa fa-group" /> Welcome to Graylog</legend>

                {alert}

                <Input ref={(username) => { this.username = username; }} id="username" type="text" placeholder="Username" autoFocus />

                <Input ref={(password) => { this.password = password; }} id="password" type="password" placeholder="Password" />

                <FormGroup>
                  <Button type="submit" bsStyle="info" disabled={loading}>
                    {loading ? 'Signing in...' : 'Sign in'}
                  </Button>
                </FormGroup>

              </form>
            </Row>
          </div>
        </div>
      </DocumentTitle>
    );
  },
});

export default LoginPage;
