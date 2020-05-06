import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Row, Col, Panel, Button } from 'components/graylog';
import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import ActionsProvider from 'injection/ActionsProvider';

const LdapActions = ActionsProvider.getActions('Ldap');

const LoginResultPanel = styled(Panel)`
  h4 {
    margin-bottom: 10px;
  }

  .login-status {
    padding: 0;
    margin-bottom: 10px;
  }

  .login-status li {
    display: inline-block;
    margin-right: 20px;
  }
`;

const StatusIcon = styled(Icon)(({ status, theme }) => `
  color: ${theme.color.variant[status]};
`);


class TestLdapLogin extends React.Component {
  static propTypes = {
    ldapSettings: PropTypes.object.isRequired,
    disabled: PropTypes.bool,
  }

  static defaultProps = {
    disabled: false,
  }

  constructor(props) {
    super(props);

    this.state = {
      loginUser: '',
      loginPassword: '',
      loginStatus: {},
    };
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { ldapSettings } = this.props;
    // Reset login status if ldapSettings changed
    if (JSON.stringify(ldapSettings) !== JSON.stringify(nextProps.ldapSettings)) {
      this.setState({ loginStatus: {} });
    }
  }

  _changeLoginForm = (event) => {
    const key = (event.target.name === 'test_login_username' ? 'loginUser' : 'loginPassword');

    this.setState({ [key]: event.target.value });
  }

  _testLogin = () => {
    const { loginUser, loginPassword } = this.state;
    const { ldapSettings } = this.props;

    LdapActions.testLogin.triggerPromise(ldapSettings, loginUser, loginPassword)
      .then(
        (result) => {
          if (result.connected && (result.login_authenticated || !ObjectUtils.isEmpty(result.entry))) {
            this.setState({ loginStatus: { loading: false, success: true, result: result } });
          } else {
            this.setState({ loginStatus: { loading: false, error: true, result: result } });
          }
        },
        () => {
          this.setState({
            loginStatus: {
              loading: false,
              error: true,
              result: {
                exception: 'Unable to test login, please try again.',
              },
            },
          });
        },
      );

    this.setState({ loginStatus: { loading: true } });
  }

  _loginTestButtonStyle = () => {
    const { loginStatus } = this.state;
    const successStyle = loginStatus.success ? 'success' : 'info';

    return loginStatus.error ? 'danger' : successStyle;
  }

  _formatLoginStatus = (loginStatus) => {
    // Don't show any status if login didn't complete
    if (!loginStatus.error && !loginStatus.success) {
      return null;
    }

    const { loginPassword } = this.state;
    const title = `Connection ${loginStatus.error ? 'failed' : 'successful'}`;
    const style = loginStatus.error ? 'danger' : 'success';

    let userFound;
    if (ObjectUtils.isEmpty(loginStatus.result.entry)) {
      userFound = <StatusIcon status="danger" name="times" />;
    } else {
      userFound = <StatusIcon status="success" name="check" />;
    }

    let loginCheck;
    if (loginStatus.result.login_authenticated) {
      loginCheck = <StatusIcon status="success" name="check" />;
    } else if (loginPassword === '') {
      loginCheck = <StatusIcon status="info" name="question" />;
    } else {
      loginCheck = <StatusIcon status="danger" name="times" />;
    }

    let serverResponse;
    if (loginStatus.result.exception) {
      serverResponse = <pre>{loginStatus.result.exception}</pre>;
    }

    const attributes = Object.keys(loginStatus.result.entry).map((key) => {
      return [
        <dt key={`${key}-dt`}>{key}</dt>,
        <dd key={`${key}-dd`}>{loginStatus.result.entry[key]}</dd>,
      ];
    });
    const formattedEntry = (attributes.length > 0 ? <dl>{attributes}</dl>
      : <p>LDAP server did not return any attributes for the user.</p>);

    const groups = (loginStatus.result.groups ? loginStatus.result.groups.map((group) => <li key={group}>{group}</li>) : []);
    const formattedGroups = (groups.length > 0 ? <ul style={{ padding: 0 }}>{groups}</ul>
      : <p>LDAP server did not return any groups for the user.</p>);

    return (
      <Row>
        <Col sm={9} smOffset={3}>
          <LoginResultPanel header={title} bsStyle={style}>
            <ul className="login-status">
              <li><h4>User found {userFound}</h4></li>
              <li><h4>Login attempt {loginCheck}</h4></li>
            </ul>
            {serverResponse && <h4>Server response</h4>}
            {serverResponse}
            <h4>User&apos;s LDAP attributes</h4>
            {formattedEntry}
            <h4>User&apos;s LDAP groups</h4>
            {formattedGroups}
          </LoginResultPanel>
        </Col>
      </Row>
    );
  }

  render() {
    const { loginStatus, loginUser, loginPassword } = this.state;
    const { disabled } = this.props;
    const loginDisabled = disabled || !loginUser || loginStatus.loading;

    const _disableSubmitOnEnter = (event) => {
      if (event.key && event.key === 'Enter') {
        event.preventDefault();
      }
    };

    return (
      <div>
        <Input id="test_login_input"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               label="Login test"
               help="Verify the previous settings by loading the entry for the given user name. If you omit the password, no authentication attempt will be made.">
          <Row className="row-sm">
            <Col sm={5}>
              <input type="text"
                     id="test_login_username"
                     name="test_login_username"
                     className="form-control"
                     value={loginUser}
                     onChange={this._changeLoginForm}
                     onKeyPress={_disableSubmitOnEnter}
                     placeholder="Username for login test"
                     disabled={disabled} />
            </Col>
            <Col sm={5}>
              <input type="password"
                     id="test_login_password"
                     name="test_login_password"
                     className="form-control"
                     value={loginPassword}
                     onChange={this._changeLoginForm}
                     onKeyPress={_disableSubmitOnEnter}
                     placeholder="Password"
                     disabled={disabled} />
            </Col>
            <Col sm={2}>
              <Button bsStyle={this._loginTestButtonStyle()}
                      disabled={loginDisabled}
                      onClick={this._testLogin}>
                {loginStatus.loading ? 'Testing...' : 'Test login'}
              </Button>
            </Col>
          </Row>
        </Input>
        {this._formatLoginStatus(loginStatus)}
      </div>
    );
  }
}


export default TestLdapLogin;
