
import React from 'react';
import { Row, Col, Input, Button, FormControls } from 'react-bootstrap';

import RolesSelect from 'components/users/RolesSelect';
import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';
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

        <Input ref="fullname" name="fullname" id="fullname" type="text" maxLength={200}
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
          <RolesSelect ref="roles" availableRoles={this.props.roles} className="form-control"/>
        </Input>

        <Input ref="session-timeout-never" type="checkbox" id="session-timeout-never" name="session_timeout_never"
               labelClassName="col-sm-10" wrapperClassName="col-sm-offset-2 col-sm-10"
               label="Sessions do not time out" help="When checked sessions never time out due to inactivity."/>

        <Input label="Timeout" help="Session automatically end after this amount of time, unless they are actively used."
               labelClassName="col-sm-2" wrapperClassName="col-sm-10">
          <Row className="row">
            <Col sm={2}>
              <input ref="timeout" type="number" id="timeout" className="session-timeout-fields validatable form-control" name="timeout" min={1} data-validate="positive_number" />
            </Col>
            <Col sm={3}>
              <TimeoutUnitSelect ref="session_timeout_unit" className="form-control session-timeout-fields" />
            </Col>
          </Row>
        </Input>

        <div className="form-group">
          <label htmlFor="timezone" className="col-sm-2 control-label">Time Zone</label>
          <Col sm={10}>
            <Row>
              <Col sm={5}>
                <TimezoneSelect ref="timezone" className="timezone-select"/>
              </Col>
            </Row>
            <span className="help-block">
              Choose your local time zone or leave it as it is to use the system's default.
            </span>
          </Col>
        </div>

        <div className="form-group">
          <Col smOffset={2} sm={10}>
            <Button type="submit" bsStyle="success" className="create-user" onClick={this._onSubmit} wrapperClassName="col-sm-offset-2 col-sm-10">
              Create User
            </Button>
          </Col>
        </div>
      </form>
    );
  },
});

export default NewUserForm;
