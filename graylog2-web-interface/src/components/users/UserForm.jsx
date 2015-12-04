import React from 'react';
import Reflux from 'reflux';
import { Input, Button, Row, Col, Alert, Panel } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';
import UserNotification from 'util/UserNotification';

import StreamsStore from 'stores/streams/StreamsStore';
import DashboardsStore from 'stores/dashboards/DashboardsStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';
import RolesStore from 'stores/users/RolesStore';
import UsersStore from 'stores/users/UsersStore';

import Spinner from 'components/common/Spinner';
import MultiSelect from 'components/common/MultiSelect';
import RolesSelect from 'components/users/RolesSelect';
import TimeoutInput from 'components/users/TimeoutInput';
import {TimezoneSelect} from 'components/common';

const UserForm = React.createClass({
  propTypes: {
    user: React.PropTypes.object.isRequired,
  },
  mixins: [PermissionsMixin, Reflux.connect(CurrentUserStore)],
  getInitialState() {
    return {
      streams: undefined,
      dashboards: undefined,
      roles: undefined,
    };
  },
  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      this.setState({
        streams: streams.sort((s1, s2) => s1.title.localeCompare(s2.title)),
      });
    });
    DashboardsStore.listDashboards().then((dashboards) => {
      this.setState({dashboards: dashboards.toArray().sort((d1, d2) => d1.title.localeCompare(d2.title))});
    });
    RolesStore.loadRoles().then((response) => {
      this.setState({roles: response.roles.sort((r1, r2) => r1.name.localeCompare(r2.name))});
    });
  },
  formatMultiselectOptions(collection) {
    return collection.map((item) => {
      return {value: item.id, label: item.title};
    });
  },
  formatSelectedOptions(permissions, permission, collection) {
    return collection
      .filter((item) => this.isPermitted(permissions, [permission + ':' + item.id]))
      .map((item) => item.id)
      .join(',');
  },
  _updateRoles(evt) {
    evt.preventDefault();
    const roles = this.refs.roles.getValue().filter((value) => value !== "");
    UsersStore.updateRoles(this.props.user.username, roles).then(() => {
      UserNotification.success('Roles updated successfully.', 'Success!');
    }, () => {
      UserNotification.error('Updating roles failed.', 'Error!');
    });
  },
  _changePassword(evt) {
    evt.preventDefault();
    const request = {};

    if (this.refs.old_password) {
      request.old_password = this.refs.old_password.getValue();
    }
    request.password = this.refs.password.getValue();

    UsersStore.changePassword(this.props.user.username, request).then(() => {
      UserNotification.success('Password updated successfully.', 'Success!');
    }, () => {
      UserNotification.error('Updating password failed.', 'Error!');
    });
  },
  _updateUser(evt) {
    evt.preventDefault();
    const request = {};
    ['full_name',
      'email',
      'session_timeout_ms',
      'timezone'].forEach((field) => {
      request[field] = this.refs[field].getValue();
    });
    UsersStore.update(this.props.user.username, request).then(() => {
      UserNotification.success('User updated successfully.', 'Success!');
    }, () => {
      UserNotification.error('Updating user failed.', 'Error!');
    });
  },
  render() {
    if (!this.state.streams || !this.state.dashboards || !this.state.roles) {
      return <Spinner />;
    }

    const user = this.props.user;
    const permissions = this.state.currentUser.permissions;
    const requiresOldPassword = !this.isPermitted(permissions, 'users:passwordchange:*');

    const streamReadOptions = this.formatSelectedOptions(this.props.user.permissions, 'streams:read', this.state.streams);
    const streamEditOptions = this.formatSelectedOptions(this.props.user.permissions, 'streams:edit', this.state.streams);

    const dashboardReadOptions = this.formatSelectedOptions(this.props.user.permissions, 'dashboards:read', this.state.dashboards);
    const dashboardEditOptions = this.formatSelectedOptions(this.props.user.permissions, 'dashboards:edit', this.state.dashboards);

    return (
      <div>
        <Row className="row content">
          <Col lg={8}>
            <h2>User information</h2>
            <form className="form-horizontal user-form" id="edit-user-form" onSubmit={this._updateUser}>
              {user.read_only &&
                <span>
                  <Col smOffset={3} sm={9}>
                    <Alert bsStyle="warning" role="alert">
                      The admin user can only be modified in your Graylog server configuration file.
                    </Alert>
                  </Col>
                  <div className="clearfix" />
                  <br />
                </span>
              }
              <fieldset disabled={user.read_only}>
                <Input ref="full_name" name="fullname" id="fullname" type="text" maxLength={200} defaultValue={user.full_name}
                       labelClassName="col-sm-3" wrapperClassName="col-sm-9"
                       label="Full Name" help="Give a descriptive name for this account, e.g. the full name." required />

                <Input ref="email" name="email" id="email" type="email" maxLength={254} defaultValue={user.email}
                       labelClassName="col-sm-3" wrapperClassName="col-sm-9"
                       label="Email Address" help="Give the contact email address." required />

                {this.isPermitted(permissions, 'USERS_EDIT') &&
                  <span>
                    <div className="form-group">
                      <Col sm={9} smOffset={3}>
                        <Panel bsStyle="danger" header="Setting individual permissions is deprecated, please consider migrating to roles instead.">
                          The permissions listed here are the result of combining all granted permissions by the roles assigned to a user,
                          which you can edit at the bottom of this page, as well as legacy, individual permissions which were assigned to the user before.
                        </Panel>
                      </Col>
                      <label className="col-sm-3 control-label" htmlFor="streampermissions">Streams Permissions</label>
                      <Col sm={9}>
                        <MultiSelect
                          ref="streamReadOptions"
                          options={this.formatMultiselectOptions(this.state.streams)}
                          value={streamReadOptions}
                          placeholder="Choose read permissions..."
                        />
                        <span className="help-block">Choose streams the user can <strong>view</strong>
                          . Removing read access will remove edit access, too.</span>
                        <MultiSelect
                          ref="streamEditOptions"
                          options={this.formatMultiselectOptions(this.state.streams)}
                          value={streamEditOptions}
                          placeholder="Choose edit permissions..."
                        />
                        <span className="help-block">Choose the streams the user can <strong>edit</strong>
                          . Values chosen here will enable read access, too.</span>
                      </Col>
                    </div>
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="dashboardpermissions">Dashboard Permissions</label>
                      <Col sm={9}>
                        <MultiSelect
                          ref="dashboardReadOptions"
                          options={this.formatMultiselectOptions(this.state.dashboards)}
                          value={dashboardReadOptions}
                          placeholder="Choose read permissions..."
                        />
                        <span className="help-block">Choose dashboards the user can <strong>view</strong>
                          . Removing read access will remove edit access, too.</span>
                        <MultiSelect
                          ref="dashboardEditOptions"
                          options={this.formatMultiselectOptions(this.state.dashboards)}
                          value={dashboardEditOptions}
                          placeholder="Choose edit permissions..."
                        />
                        <span className="help-block">Choose dashboards the user can <strong>edit</strong>
                          . Values chosen here will enable read access, too.</span>
                      </Col>
                    </div>
                  </span>
                }
                {this.isPermitted(permissions, '*') &&
                  <TimeoutInput ref="session_timeout_ms" value={user.session_timeout_ms} labelSize={3} controlSize={9} />
                }

                <Input label="Time Zone" help="Choose your local time zone or leave it as it is to use the system's default."
                       labelClassName="col-sm-3" wrapperClassName="col-sm-9">
                  <TimezoneSelect ref="timezone" className="timezone-select" value={user.timezone}/>
                </Input>

                <div className="form-group">
                  <Col smOffset={3} sm={9}>
                    <Button type="submit" bsStyle="success" className="create-user">
                      Update User
                    </Button>
                  </Col>
                </div>
              </fieldset>
            </form>
          </Col>
        </Row>
        <Row className="content">
          <Col lg={8}>
            <h2>Change password</h2>
            {user.read_only ?
            <Col smOffset={3} sm={9}>
              <Alert bsStyle="warning" role="alert">
                Please edit your Graylog server configuration file to change the admin password.
              </Alert>
            </Col>
            :
              user.external ?
              <Col smOffset={3} sm={9}>
                <Alert bsStyle="warning" role="alert">
                  This user was created from an external system and you can't change the password here.
                  Please contact an administrator for more information.
                </Alert>
              </Col>
              :
              <form className="form-horizontal" style={{marginTop: 10}} onSubmit={this._changePassword}>
                {requiresOldPassword &&
                  <Input ref="old_password" name="old_password" id="old_password" type="password" maxLength={100}
                         labelClassName="col-sm-3" wrapperClassName="col-sm-9"
                         label="Old Password" required />
                }
                <Input ref="password" name="password" id="password" type="password" maxLength={100}
                       labelClassName="col-sm-3" wrapperClassName="col-sm-9"
                       label="New Password" help="Passwords must be at least 6 characters long. We recommend using a strong password." required />

                <Input ref="password_repeat" name="password_repeat" id="password_repeat" type="password" maxLength={100}
                       labelClassName="col-sm-3" wrapperClassName="col-sm-9"
                       label="Repeat Password" required />

                <div className="form-group">
                  <Col smOffset={3} sm={9}>
                    <Button bsStyle="success" type="submit">
                      Update Password
                    </Button>
                  </Col>
                </div>
              </form>
            }
          </Col>
        </Row>
        {this.isPermitted(permissions, 'USERS_ROLESEDIT') &&
          <Row className="content">
            <Col lg={8}>
              <h2>Change user role</h2>
              {user.read_only ?
                <Col smOffset={3} sm={9}>
                  <Alert bsStyle="warning" role="alert">
                    You cannot edit the admin's user role.
                  </Alert>
                </Col>
              :
                <span>
                  {user.external &&
                    <Col smOffset={3} sm={9} style={{marginBottom: 15}}>
                      <Alert bsStyle="warning" role="alert">
                        This user was created from an external LDAP system, please consider mapping LDAP groups instead of manually editing roles here.
                        Please update the LDAP group mapping to make changes or contact an administrator for more information.
                      </Alert>
                    </Col>
                  }
                  <form className="form-horizontal" style={{marginTop: '10px'}} onSubmit={this._updateRoles}>
                    <Input label="Roles" help="Choose the roles the user should be a member of. All the granted permissions will be combined."
                           labelClassName="col-sm-3" wrapperClassName="col-sm-9">
                      <RolesSelect ref="roles" userRoles={user.roles} availableRoles={this.state.roles} />
                    </Input>
                    <div className="form-group">
                      <Col smOffset={3} sm={9}>
                        <Button bsStyle="success" type="submit" data-confirm={'Really update roles for ' + user.username + '?'}>
                          Update role
                        </Button>
                      </Col>
                    </div>
                  </form>
                </span>
              }
            </Col>
          </Row>
        }
      </div>
    );
  },
});

export default UserForm;
