
import React from 'react';
import { Row, Col, Input, Button, FormControls } from 'react-bootstrap';

import RolesSelect from 'components/users/RolesSelect';
import TimeoutInput from 'components/users/TimeoutInput';
import TimezoneSelect from 'components/users/TimezoneSelect';

const NewUserForm = React.createClass({
  propTypes: {
    roles: React.PropTypes.array.isRequired,
    onSubmit: React.PropTypes.func.isRequired,
  },
  _onSubmit(evt) {
    evt.preventDefault();
    const result = {};
    Object.keys(this.refs).forEach((ref) => {
      if (ref !== 'password_repeat') {
        result[ref] = (this.refs[ref].getValue ? this.refs[ref].getValue() : this.refs[ref].value);
      }
    });

    this.props.onSubmit(result);
  },
  render() {
    const rolesHelp = (
      <span className="help-block">
        Assign the relevant roles to this user to grant them access to the relevant streams and dashboards.<br />
              The <em>Reader</em> role grants basic access to the system and will be enabled.<br />
              The <em>Admin</em> role grants access to everything in Graylog.
      </span>
    );
    return (
      <form id="create-user-form" className="form-horizontal">
        <Input ref="username" name="username" id="username" type="text" maxLength={100}
               labelClassName="col-sm-2" wrapperClassName="col-sm-10"
               label="Username" help="Select a unique user name used to log in with." required />

        <Input ref="full_name" name="fullname" id="fullname" type="text" maxLength={200}
               labelClassName="col-sm-2" wrapperClassName="col-sm-10"
               label="Full Name" help="Give a descriptive name for this account, e.g. the full name." required />

        <Input ref="email" name="email" id="email" type="email" maxLength={254}
               labelClassName="col-sm-2" wrapperClassName="col-sm-10"
               label="Email Address" help="Give the contact email address." required />

        <Input label="Password" help="Passwords must be at least 6 characters long. We recommend using a strong password."
               labelClassName="col-sm-2" wrapperClassName="col-sm-10">
          <Row>
            <Col sm={6}>
              <input className="form-control" ref="password" name="password" id="password" type="password" placeholder="Password" required />
            </Col>
            <Col sm={6}>
              <input className="form-control" ref="password_repeat" id="password-repeat" type="password" placeholder="Repeat password" required />
            </Col>
          </Row>
        </Input>

        <Input label="Roles" help={rolesHelp}
               labelClassName="col-sm-2" wrapperClassName="col-sm-10">
          <RolesSelect ref="roles" availableRoles={this.props.roles} userRoles={['Reader']} className="form-control"/>
        </Input>

        <TimeoutInput ref="session_timeout_ms" />

        <Input label="Time Zone" help="Choose your local time zone or leave it as it is to use the system's default."
               labelClassName="col-sm-2" wrapperClassName="col-sm-10">
          <TimezoneSelect ref="timezone" className="timezone-select"/>
        </Input>

        <div className="form-group">
          <Col smOffset={2} sm={10}>
            <Button type="submit" bsStyle="success" className="create-user" onClick={this._onSubmit}>
              Create User
            </Button>
          </Col>
        </div>
      </form>
    );
  },
});

export default NewUserForm;
