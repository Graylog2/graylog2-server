import PropTypes from 'prop-types';
import React from 'react';
import { Alert, Row, Col, Button } from 'components/graylog';

import { Input } from 'components/bootstrap';
import RolesSelect from 'components/users/RolesSelect';
import TimeoutInput from 'components/users/TimeoutInput';
import { TimezoneSelect } from 'components/common';

import StoreProvider from 'injection/StoreProvider';

import ValidationsUtils from 'util/ValidationsUtils';

const UsersStore = StoreProvider.getStore('Users');

class NewUserForm extends React.Component {
  static propTypes = {
    roles: PropTypes.array.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
  };

  state = {
    users: [],
    newRoles: null,
  };

  componentDidMount() {
    UsersStore.loadUsers().then((users) => {
      this.setState({ users });
    });
  }

  inputs = {};

  _onUsernameChange = (event) => {
    const usernameField = this.inputs.username.getInputDOMNode();
    const usernameExists = this.state.users.some(user => user.username === event.target.value);

    ValidationsUtils.setFieldValidity(usernameField, usernameExists, 'Username is already taken');
  };

  _onPasswordChange = () => {
    const passwordField = this.inputs.password;
    const passwordConfirmField = this.inputs.password_repeat;

    if (passwordField.value !== '' && passwordConfirmField.value !== '') {
      ValidationsUtils.setFieldValidity(passwordConfirmField, passwordField.value !== passwordConfirmField.value, 'Passwords do not match');
    }
  };

  _onSubmit = (evt) => {
    evt.preventDefault();
    const result = {};
    Object.keys(this.inputs).forEach((ref) => {
      if (ref !== 'password_repeat') {
        result[ref] = (this.inputs[ref].getValue ? this.inputs[ref].getValue() : this.inputs[ref].value);
      }
    });

    this.props.onSubmit(result);
  };

  _onValueChange = (newRoles) => {
    const roles = newRoles.split(',');
    this.setState({ newRoles: roles });
  };

  render() {
    const rolesHelp = (
      <span className="help-block">
        Assign the relevant roles to this user to grant them access to the relevant streams and dashboards.<br />
        The <em>Reader</em> role grants basic access to the system and will be enabled.<br />
        The <em>Admin</em> role grants access to everything in Graylog.
      </span>
    );
    const roles = this.state.newRoles;
    let rolesAlert = null;
    if (roles != null && !(roles.includes('Reader') || roles.includes('Admin'))) {
      rolesAlert = (
        <Alert bsStyle="danger" role="alert">
          You need to select at least one of the <em>Reader</em> or <em>Admin</em> roles.
        </Alert>
      );
    }
    return (
      <form id="create-user-form" className="form-horizontal" onSubmit={this._onSubmit}>
        <Input ref={(elem) => { this.inputs.username = elem; }}
               name="username"
               id="username"
               type="text"
               maxLength={100}
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10"
               label="Username"
               help="Select a unique user name used to log in with."
               required
               onChange={this._onUsernameChange}
               autoFocus />

        <Input ref={(elem) => { this.inputs.full_name = elem; }}
               name="fullname"
               id="fullname"
               type="text"
               maxLength={200}
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10"
               label="Full Name"
               help="Give a descriptive name for this account, e.g. the full name."
               required />

        <Input ref={(elem) => { this.inputs.email = elem; }}
               name="email"
               id="email"
               type="email"
               maxLength={254}
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10"
               label="Email Address"
               help="Give the contact email address."
               required />

        <Input id="password-field"
               label="Password"
               help="Passwords must be at least 6 characters long. We recommend using a strong password."
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10">
          <Row>
            <Col sm={6}>
              <input className="form-control"
                     ref={(elem) => { this.inputs.password = elem; }}
                     name="password"
                     id="password"
                     type="password"
                     placeholder="Password"
                     required
                     minLength="6"
                     onChange={this._onPasswordChange} />
            </Col>
            <Col sm={6}>
              <input className="form-control"
                     ref={(elem) => { this.inputs.password_repeat = elem; }}
                     id="password-repeat"
                     type="password"
                     placeholder="Repeat password"
                     required
                     minLength="6"
                     onChange={this._onPasswordChange} />
            </Col>
          </Row>
        </Input>

        <Input id="roles-select"
               label="Roles"
               help={rolesHelp}
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10">
          <span>
            <RolesSelect ref={(elem) => { this.inputs.roles = elem; }}
                         availableRoles={this.props.roles}
                         userRoles={['Reader']}
                         className="form-control"
                         onValueChange={this._onValueChange} />
            {rolesAlert}
          </span>
        </Input>

        <TimeoutInput ref={(elem) => { this.inputs.session_timeout_ms = elem; }} />

        <Input id="timezone-select"
               label="Time Zone"
               help="Choose the timezone to use to display times, or leave it as it is to use the system's default."
               labelClassName="col-sm-2"
               wrapperClassName="col-sm-10">
          <TimezoneSelect ref={(elem) => { this.inputs.timezone = elem; }} className="timezone-select" />
        </Input>

        <div className="form-group">
          <Col smOffset={2} sm={10}>
            <Button type="submit" bsStyle="primary" className="create-user save-button-margin" disabled={!!rolesAlert}>
              Create User
            </Button>
            <Button onClick={this.props.onCancel}>Cancel</Button>
          </Col>
        </div>
      </form>
    );
  }
}

export default NewUserForm;
