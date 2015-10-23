import React from 'react';
import Reflux from 'reflux';

import PermissionsMixin from 'util/PermissionsMixin';

import StreamsStore from 'stores/streams/StreamsStore';
import DashboardStore from 'stores/dashboard/DashboardStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';
import RolesStore from 'stores/users/RolesStore';

import Spinner from 'components/common/Spinner';
import MultiSelect from 'components/common/MultiSelect';

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
        streams: streams,
      });
    });
    DashboardStore.listDashboards().then((dashboards) => {
      this.setState({dashboards: dashboards.toArray()});
    });
    RolesStore.loadRoles().then((response) => {
      this.setState({roles: response.roles});
    });
  },
  formatPermissionOptions(permissions, permission, streams) {
    return streams
      .sort((s1, s2) => s1.title.localeCompare(s2.title))
      .map((stream) => {
        return <option key={permissions + stream.id} value={stream.id}>{stream.title}</option>;
      });
  },
  formatTimezoneOptions() {
    /*                           @for(group &lt;- ImmutableList.copyOf(timezones.keys).sorted) {
     <option />
     <optgroup label="@group">
     @for(zone &lt;- ImmutableList.copyOf(timezones.get(group).iterator()).sorted) {
     @if(form("timezone").valueOr("").equals(group + "/" + zone)) {
     <option value="@group/@zone" selected>@zone</option>
     } else {
     <option value="@group/@zone">@zone</option>
     }
     }
     </optgroup>
     }
     */
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
  handleMultiselectChange(foo, bar, baz) {
    console.log([foo, bar, baz]);
  },
  render() {
    if (!this.state.streams || !this.state.dashboards || !this.state.roles) {
      return <Spinner />;
    }

    const user = this.props.user;
    const userPermissions = user.permissions;
    const permissions = this.state.currentUser.permissions;
    const requiresOldPassword = !this.isPermitted(permissions, 'users:passwordchange:*');

    const streamReadOptions = this.formatSelectedOptions(this.props.user.permissions, 'streams:read', this.state.streams);
    const streamEditOptions = this.formatSelectedOptions(this.props.user.permissions, 'streams:edit', this.state.streams);

    const dashboardReadOptions = this.formatSelectedOptions(this.props.user.permissions, 'dashboards:read', this.state.dashboards);
    const dashboardEditOptions = this.formatSelectedOptions(this.props.user.permissions, 'dashboards:edit', this.state.dashboards);

    const timezoneOptions = null;

    const rolesOptions = this.state.roles.map((role) => {
      return {value: role.name, label: role.name};
    });

    return (
      <div>
        <div className="row content">
          <div className="col-lg-8">
            <h2>User information</h2>
            <form className="form-horizontal user-form" id="edit-user-form" action="@routes.UsersController.saveChanges(username)" method="POST">
              {user.read_only &&
                <span>
                  <div className="col-sm-offset-3 col-sm-9">
                    <div className="alert alert-warning" role="alert">
                      The admin user can only be modified in your Graylog server configuration file.
                    </div>
                  </div>
                  <div className="clearfix" />
                  <br />
                </span>
              }
              <fieldset disabled={user.read_only}>
                <div className="form-group">
                  <label htmlFor="fullname" className="col-sm-3 control-label">Full Name</label>
                  <div className="col-sm-9">
                    <input ref="fullname" name="fullname" id="fullname" className="form-control" type="text" defaultValue={user.full_name} maxLength={200} required />
                    <span className="help-block">Give a descriptive name for this account, e.g. the full name.</span>
                  </div>
                </div>
                <div className="form-group">
                  <label htmlFor="email" className="col-sm-3 control-label">Email Address</label>
                  <div className="col-sm-9">
                    <input ref="email" name="email" id="email" className="form-control" type="email" defaultValue={user.email} maxLength={254} required />
                    <span className="help-block">Give the contact email address.</span>
                  </div>
                </div>
                {this.isPermitted(permissions, 'USERS_EDIT') &&
                  <span>
                    <div className="form-group">
                      <div className="col-sm-9 col-sm-offset-3">
                        <div className="panel panel-danger">
                          <div className="panel-heading">Setting individual permissions is deprecated, please consider migrating to roles instead.</div>
                          <div className="panel-body">
                            The permissions listed here are the result of combining all granted permissions by the roles assigned to a user,
                            which you can edit at the bottom of this page, as well as legacy, individual permissions which were assigned to the user before.
                          </div>
                        </div>
                      </div>
                      <label className="col-sm-3 control-label" htmlFor="streampermissions">Streams Permissions</label>
                      <div className="col-sm-9">
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
                      </div>
                    </div>
                    <div className="form-group">
                      <label className="col-sm-3 control-label" htmlFor="dashboardpermissions">Dashboard Permissions</label>
                      <div className="col-sm-9">
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
                      </div>
                    </div>
                  </span>
                }
                {this.isPermitted(permissions, '*') &&
                  <span>
                    <div className="form-group">
                      <div className="col-sm-offset-3 col-sm-9">
                        <div className="checkbox">
                          <label>
                            <input ref="session_timeout_never" type="checkbox" id="session-timeout-never" name="session_timeout_never" checked={user.sessions_never_timeout} />
                            Sessions do not time out
                          </label>
                        </div>
                        <span className="help-block">When checked sessions never time out due to inactivity.</span>
                      </div>
                    </div>
                    <div className="form-group">
                      <label htmlFor="timeout" className="col-sm-3 control-label">Timeout</label>
                      <div className="col-sm-9">
                        <div className="row">
                          <div className="col-sm-3">
                            <input ref="session_timeout" type="number" id="timeout" className="session-timeout-fields validatable form-control" name="timeout" min={1} data-validate="positive_number" defaultValue={user.timeout} />
                          </div>
                          <div className="col-sm-3">
                            <select ref="session_timeout_unit" name="timeout_unit" id="timeout_unit" className="session-timeout-fields form-control">
                              <option value={1000}>Seconds</option>
                              <option value={60*1000}>Minutes</option>
                              <option value={60*60*1000}>Hours</option>
                              <option value={24*60*60*1000}>Days</option>
                            </select>
                          </div>
                        </div>
                        <span className="help-block">
                          Session automatically end after this amount of time, unless they are actively used.
                        </span>
                      </div>
                    </div>
                  </span>
                }
                <div className="form-group">
                  <label htmlFor="timezone" className="col-sm-3 control-label">Time Zone</label>
                  <div className="col-sm-9">
                    <div className="row">
                      <div className="col-sm-5">
                        <select ref="timezone" name="timezone" id="timezone" data-placeholder="Pick your time zone" className="timezone-select">
                          {timezoneOptions}
                        </select>
                      </div>
                    </div>
                    <span className="help-block">
                      Choose your local time zone or leave it as it is to use the system's default.
                    </span>
                  </div>
                </div>
                <div className="form-group">
                  <div className="col-sm-offset-3 col-sm-9">
                    <button type="submit" className="btn btn-success create-user">
                      Update User
                    </button>
                  </div>
                </div>
              </fieldset>
            </form>
          </div>
        </div>
        <div className="row content">
          <div className="col-lg-8">
            <h2>Change password</h2>
            {user.read_only ?
            <div className="col-sm-offset-3 col-sm-9">
              <div className="alert alert-warning" role="alert">
                Please edit your Graylog server configuration file to change the admin password.
              </div>
            </div>
            :
              user.external ?
              <div className="col-sm-offset-3 col-sm-9">
                <div className="alert alert-warning" role="alert">
                  This user was created from an external system and you can't change the password here.
                  Please contact an administrator for more information.
                </div>
              </div>
              :
              <form className="form-horizontal" style={{marginTop: 10}} action="@routes.UsersController.changePassword(username)" method="POST">
                {requiresOldPassword &&
                  <div className="form-group">
                    <label className="col-sm-3 control-label" htmlFor="old-password">Old Password</label>
                    <div className="col-sm-9">
                      <input ref="old_password" type="password" id="old-password" name="old_password" className="form-control" required/>
                    </div>
                  </div>
                }
                <div className="form-group">
                  <label className="col-sm-3 control-label" htmlFor="password">New Password</label>
                  <div className="col-sm-9">
                    <input ref="password" type="password" id="password" name="password" className="form-control" required />
                    <span className="help-block">
                      Passwords must be at least 6 characters long. We recommend using a strong password.
                    </span>
                  </div>
                </div>
                <div className="form-group">
                  <label className="col-sm-3 control-label" htmlFor="password-repeat">Repeat Password</label>
                  <div className="col-sm-9">
                    <input ref="password_repeat" type="password" id="password-repeat" className="form-control" required />
                  </div>
                </div>
                <div className="form-group">
                  <div className="col-sm-offset-3 col-sm-9">
                    <button className="btn btn-success" type="submit">
                      Update Password
                    </button>
                  </div>
                </div>
              </form>
            }
          </div>
        </div>
        {this.isPermitted(permissions, 'USERS_ROLESEDIT') &&
          <div className="row content">
            <div className="col-lg-8">
              <h2>Change user role</h2>
              {user.read_only ?
                <div className="col-sm-offset-3 col-sm-9">
                  <div className="alert alert-warning" role="alert">
                    You cannot edit the admin's user role.
                  </div>
                </div>
              :
                <span>
                  {user.external &&
                    <div className="col-sm-offset-3 col-sm-9" style={{marginBottom: 15}}>
                      <div className="alert alert-warning" role="alert">
                        This user was created from an external LDAP system, please consider mapping LDAP groups instead of manually editing roles here.
                        Please update the LDAP group mapping to make changes or contact an administrator for more information.
                      </div>
                    </div>
                  }
                  <form className="form-horizontal" style={{marginTop : '10 px'}} action="@routes.UsersController.updateRoles(user.getName)" method="POST">
                    <div className="form-group">
                      <label className="col-sm-3 control-label">User Roles</label>
                      <div className="col-sm-9">
                        <MultiSelect
                          ref="rolesOptions"
                          options={rolesOptions}
                          value={user.roles.join(',')}
                          placeholder="Choose roles..."
                        />
                        <span className="help-block">
                          Choose the roles the user should be a member of. All the granted permissions will be combined.</span>
                      </div>
                    </div>
                    <div className="form-group">
                      <div className="col-sm-offset-3 col-sm-9">
                        <button className="btn btn-success" type="submit" data-confirm={'Really update roles for ' + user.username + '?'}>
                          Update role
                        </button>
                      </div>
                    </div>
                  </form>
                </span>
              }
            </div>
          </div>
        }
      </div>
    );
  },
});

export default UserForm;
