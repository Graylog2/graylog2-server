import React from 'react';
import Reflux from 'reflux';
import { Row, Input, ButtonInput, Alert } from 'react-bootstrap';
import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';

import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';
import authStyle from '!style/useable!css!less!stylesheets/auth.less';

const LoginPage = React.createClass({
  mixins: [Reflux.connect(SessionStore), Reflux.ListenerMethods],
  componentDidMount() {
    disconnectedStyle.use();
    authStyle.use();
  },
  componentWillUnmount() {
    disconnectedStyle.unuse();
    authStyle.unuse();
  },

  onSignInClicked(event) {
    event.preventDefault();
    this.resetLastError();
    const username = this.refs.username.getValue();
    const password = this.refs.password.getValue();
    const location = document.location.host;
    SessionActions.login.triggerPromise(username, password, location).catch((error) => {
      if (error.additional.status === 401) {
        this.setState({lastError: 'Invalid credentials, please verify them and retry.'});
      } else {
        this.setState({lastError: 'Error - the server returned: ' + error.additional.status + ' - ' + error.message});
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
    this.setState({lastError: undefined});
  },
  render() {
    const alert = this.formatLastError(this.state.lastError);
    return (
      <div>
        <div className="container" id="login-box">
          <Row>
            <form className="col-md-4 col-md-offset-4 well" id="login-box-content" onSubmit={this.onSignInClicked}>
              <legend><i className="fa fa-group"/> Welcome to Graylog</legend>

              {alert}

              <Input ref="username" type="text" placeholder="Username" autoFocus />

              <Input ref="password" type="password" placeholder="Password" />

              <ButtonInput type="submit" bsStyle="info">Sign in</ButtonInput>

            </form>
          </Row>
        </div>
      </div>
    );
  },
});

export default LoginPage;

