'use strict';

var React = require('react');
var Immutable = require('immutable');

var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;

var RoleList = require('./RoleList');
var EditRole = require('./EditRole');

var RolesStore = require('../../stores/users/RolesStore').RolesStore;
var UserNotification = require('../../util/UserNotification');

var StreamsStore = require('../../stores/streams/StreamsStore');
var DashboardStore = require('../../stores/dashboard/DashboardStore');

var RolesComponent = React.createClass({

  getInitialState() {
    return {
      roles: Immutable.Set(),
      rolesLoaded: false,
      editRole: null,
      streams: Immutable.List(),
      dashboards: Immutable.List(),
    };
  },
  componentDidMount() {
    this.loadRoles();
    StreamsStore.load(streams => this.setState({streams: Immutable.List(streams)}));
    DashboardStore.listDashboards().done(dashboards => this.setState({dashboards: dashboards}));
  },

  loadRoles() {
    var promise = RolesStore.loadRoles();
    promise.done((roles) => {
      this.setState({roles: Immutable.Set(roles), rolesLoaded: true});
    });
  },

  _showCreateRole() {
    this.setState({showEditRole: true});
  },
  _showEditRole(role) {
    this.setState({showEditRole: true, editRole: role});
  },
  _deleteRole(role) {
    if (window.confirm("Do you really want to delete role " + role.name + "?")) {
      RolesStore.getMembers(role.name).done((membership) => {
        if (membership.users.length != 0) {
          UserNotification.error("Cannot delete role " + role.name + ". It is still assigned to " + membership.users.length + " users.");
        } else {
          RolesStore.deleteRole(role.name).done(this.loadRoles);
        }
      });
    }
  },
  _saveRole(initialName, role) {
    if (initialName === null) {
      RolesStore.createRole(role).done(this._clearEditRole).done(this.loadRoles);
    } else {
      RolesStore.updateRole(initialName, role).done(this._clearEditRole).done(this.loadRoles);
    }
  },
  _clearEditRole() {
    this.setState({showEditRole: false, editRole: null});
  },

  render() {
    var content = null;
    if (!this.state.rolesLoaded) {
      content = <span>Loading roles...</span>;
    } else if (this.state.showEditRole) {
      content =
        (<EditRole initialRole={this.state.editRole} streams={this.state.streams} dashboards={this.state.dashboards}
                  onSave={this._saveRole} cancelEdit={this._clearEditRole}/>);
    } else {
      content = (<RoleList roles={this.state.roles}
                          showEditRole={this._showEditRole}
                          deleteRole={this._deleteRole}
                          createRole={this._showCreateRole}/>);
    }
    return (
      <Row>
        <Col md={12}>
          {content}
        </Col>
      </Row>
    );
  }
});


module.exports = RolesComponent;