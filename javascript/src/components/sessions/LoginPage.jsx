import React from 'react';
import { Row, Input, Button, ButtonInput } from 'react-bootstrap';
import SessionActions from 'actions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';

const LoginPage = React.createClass({
  getInitialState() {
    return {
      username: '',
      password: '',
    };
  },
  onSignInClicked() {
    SessionActions.login(this.state.username, this.state.password, 'localhost:9000');
  },
  onChangeUsername(e) {
    this.setState({username: e.target.value});
  },
  onChangePassword(e) {
    this.setState({password: e.target.value});
  },
  render() {
    return (
      <div className="container" id="login-box">
          <Row>
              <div className="col-md-4 col-md-offset-4 well" id="login-box-content">
                  <legend><i className="fa fa-group"></i> Welcome to Graylog</legend>

                  <div className="form-group">
                      <Input type="text" placeholder="Username" value={this.state.username} onChange={this.onChangeUsername} autoFocus />
                  </div>

                  <div className="form-group">
                      <Input type="password" placeholder="Password" value={this.state.password} onChange={this.onChangePassword} />
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
});

export default LoginPage;

