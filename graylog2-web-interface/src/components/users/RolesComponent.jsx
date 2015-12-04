import React from 'react';
import Immutable from 'immutable';
import { Row, Col } from 'react-bootstrap';

import StreamsStore from 'stores/streams/StreamsStore';
import DashboardsStore from 'stores/dashboards/DashboardsStore';
import RolesStore from 'stores/users/RolesStore';

import UserNotification from 'util/UserNotification';
import RoleList from 'components/users/RoleList';
import EditRole from 'components/users/EditRole';

const RolesComponent = React.createClass({
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
    DashboardsStore.listDashboards().then(dashboards => this.setState({dashboards: dashboards}));
  },

  loadRoles() {
    const promise = RolesStore.loadRoles();
    promise.then((resp) => {
      this.setState({roles: Immutable.Set(resp.roles), rolesLoaded: true});
    });
  },

  _showCreateRole() {
    this.setState({showEditRole: true});
  },
  _showEditRole(role) {
    this.setState({showEditRole: true, editRole: role});
  },
  _deleteRole(role) {
    if (window.confirm('Do you really want to delete role ' + role.name + '?')) {
      RolesStore.getMembers(role.name).then((membership) => {
        if (membership.users.length !== 0) {
          UserNotification.error('Cannot delete role ' + role.name + '. It is still assigned to ' + membership.users.length + ' users.');
        } else {
          RolesStore.deleteRole(role.name).then(this.loadRoles);
        }
      });
    }
  },
  _saveRole(initialName, role) {
    if (initialName === null) {
      RolesStore.createRole(role).then(this._clearEditRole).then(this.loadRoles);
    } else {
      RolesStore.updateRole(initialName, role).then(this._clearEditRole).then(this.loadRoles);
    }
  },
  _clearEditRole() {
    this.setState({showEditRole: false, editRole: null});
  },

  render() {
    let content = null;
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
  },
});

export default RolesComponent;
