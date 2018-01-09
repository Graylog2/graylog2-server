import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';
import { Row, Button, FormGroup, Alert } from 'react-bootstrap';
import { DocumentTitle } from 'components/common';

import { Input } from 'components/bootstrap';
import LoadingPage from './LoadingPage';

import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';
import authStyle from '!style/useable!css!less!stylesheets/auth.less';

const LoginPage = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    isValidatingSession: PropTypes.bool.isRequired,
    error: PropTypes.string,
    login: PropTypes.func.isRequired,
    validateSession: PropTypes.func.isRequired,
  },

  componentDidMount() {
    disconnectedStyle.use();
    authStyle.use();
    this.props.validateSession();
  },
  componentWillUnmount() {
    disconnectedStyle.unuse();
    authStyle.unuse();
  },

  onSignInClicked(event) {
    event.preventDefault();
    const username = this.refs.username.getValue();
    const password = this.refs.password.getValue();
    const location = document.location.host;
    this.props.login(username, password, location);
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
  render() {
    if (this.props.isValidatingSession) {
      return (
        <LoadingPage />
      );
    }

    const alert = this.formatLastError(this.props.error);
    return (
      <DocumentTitle title="Sign in">
        <div>
          <div className="container" id="login-box">
            <Row>
              <form className="col-md-4 col-md-offset-4 well" id="login-box-content" onSubmit={this.onSignInClicked}>
                <legend><i className="fa fa-group" /> Welcome to Graylog</legend>

                {alert}

                <Input ref="username" id="username" type="text" placeholder="Username" autoFocus />

                <Input ref="password" id="password" type="password" placeholder="Password" />

                <FormGroup>
                  <Button type="submit" bsStyle="info" disabled={this.props.isLoading}>
                    {this.props.isLoading ? 'Signing in...' : 'Sign in'}
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

export default inject(context => ({
  isLoading: context.rootStore.sessionStore.isLoading,
  isValidatingSession: context.rootStore.sessionStore.isValidatingSession,
  error: context.rootStore.sessionStore.error,
  login: (username, password, host) => context.rootStore.sessionStore.login(username, password, host),
  validateSession: () => context.rootStore.sessionStore.validate(),
}))(observer(LoginPage));

