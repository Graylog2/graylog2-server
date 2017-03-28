import React from 'react';
import Reflux from 'reflux';
import { Row, Button, FormGroup, Alert } from 'react-bootstrap';
import { DocumentTitle } from 'components/common';

import { Input } from 'components/bootstrap';
import LoadingPage from './LoadingPage';

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');
import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');

import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';
import authStyle from '!style/useable!css!less!stylesheets/auth.less';

const LoginPage = React.createClass({
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
  },

  onSignInClicked(event) {
    event.preventDefault();
    this.resetLastError();
    this.setState({ loading: true });
    const username = this.refs.username.getValue();
    const password = this.refs.password.getValue();
    const location = document.location.host;
    const promise = SessionActions.login.triggerPromise(username, password, location);
    promise.catch((error) => {
      if (error.additional.status === 401) {
        this.setState({ lastError: 'Invalid credentials, please verify them and retry.' });
      } else {
        this.setState({ lastError: `Error - the server returned: ${error.additional.status} - ${error.message}` });
      }
    });
    promise.finally(() => {
      if (this.isMounted()) {
        this.setState({ loading: false });
      }
    });
  },
  formatLastError(error) {
    if (error) {
      return (
        <div className="form-group">
          <Alert bsStyle="danger">
            <a className="close" onClick={this.resetLastError}>×</a>{error}
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
    if (this.state.validatingSession) {
      return (
        <LoadingPage />
      );
    }

    const alert = this.formatLastError(this.state.lastError);
    return (
      <DocumentTitle title="Sign in">
        <div>
          <div className="container" id="login-box">
            <Row>
              <form className="col-md-4 col-md-offset-4 well" id="login-box-content" onSubmit={this.onSignInClicked}>
                <legend><i className="fa fa-group" /> Welcome to Graylog</legend>

                {alert}

                <Input ref="username" type="text" placeholder="Username" autoFocus />

                <Input ref="password" type="password" placeholder="Password" />

                <FormGroup>
                  <Button type="submit" bsStyle="info" disabled={this.state.loading}>
                    {this.state.loading ? 'Signing in...' : 'Sign in'}
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

