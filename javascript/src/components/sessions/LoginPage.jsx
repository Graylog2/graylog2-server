import React from 'react';
import Reflux from 'reflux';
import { Row, Input, Button, ButtonInput, Alert } from 'react-bootstrap';
import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';

const LoginPage = React.createClass({
  mixins: [Reflux.connect(SessionStore), Reflux.ListenerMethods],

  onSignInClicked() {
    const username = this.refs.username.getValue();
    const password = this.refs.password.getValue();
    SessionActions.login.triggerPromise(username, password, 'localhost:9000').catch((error) => {
      this.setState({lastError: error.message});
    });
  },
  render() {
    const alert = this.formatLastError(this.state.lastError);
    return (
      <div className="container" id="login-box">
          <Row>
              <div className="col-md-4 col-md-offset-4 well" id="login-box-content">
                  <legend><i className="fa fa-group"></i> Welcome to Graylog</legend>

                  {alert}

                  <div className="form-group">
                      <Input ref="username" type="text" placeholder="Username" autoFocus />
                  </div>

                  <div className="form-group">
                      <Input ref="password" type="password" placeholder="Password" />
                  </div>

                  <ButtonInput type="submit" bsStyle="info" onClick={this.onSignInClicked}>Sign in</ButtonInput>

                  <div className="login-advanced">
                      <div className="footer pull-right">
                          <span id="total-count-zero" className="hidden">No configured node was ever reached.</span>
                          <span id="total-count-nonzero"><span id="connected-count"></span> of <span id="total-count"></span> nodes connected.</span>
                      </div>
                      <br style={{clear: 'both'}} />
                  </div>
              </div>
          </Row>
      </div>
    );
  },
  formatLastError(error) {
    if (error) {
      return (<Alert bsStyle="danger">
        <a className="close" onClick={this.resetLastError}>×</a>{error}
      </Alert>);
    }
    return null;
  },
  resetLastError() {
    this.setState({lastError: undefined});
  },
});

export default LoginPage;

