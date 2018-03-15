import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Immutable from 'immutable';
import { Button, Col, Row } from 'react-bootstrap';

import UserNotification from 'util/UserNotification';
import RoleList from 'components/users/RoleList';
import EditRole from 'components/users/EditRole';
import PageHeader from 'components/common/PageHeader';

import CombinedProvider from 'injection/CombinedProvider';

const { StreamsStore } = CombinedProvider.get('Streams');
const { DashboardsActions, DashboardsStore } = CombinedProvider.get('Dashboards');
const { RolesStore } = CombinedProvider.get('Roles');


export default createReactClass({
  displayName: 'RolesComponent',
  mixins: [Reflux.connect(DashboardsStore, 'dashboards')],

  getInitialState() {
    return {
      roles: Immutable.Set(),
      rolesLoaded: false,
      editRole: null,
      streams: Immutable.List(),
    };
  },

  componentDidMount() {
    this.loadRoles();
    StreamsStore.load(streams => this.setState({ streams: Immutable.List(streams) }));
    DashboardsActions.list();
  },

  loadRoles() {
    const promise = RolesStore.loadRoles();
    promise.then((roles) => {
      this.setState({ roles: Immutable.Set(roles), rolesLoaded: true });
    });
  },

  _showCreateRole() {
    this.setState({ showEditRole: true });
  },

  _showEditRole(role) {
    this.setState({ showEditRole: true, editRole: role });
  },

  _deleteRole(role) {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete role ${role.name}?`)) {
      RolesStore.getMembers(role.name).then((membership) => {
        if (membership.users.length !== 0) {
          UserNotification.error(`Cannot delete role ${role.name}. It is still assigned to ${membership.users.length} users.`);
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
    this.setState({ showEditRole: false, editRole: null });
  },

  render() {
    let content = null;
    if (!this.state.rolesLoaded) {
      content = <span>Loading roles...</span>;
    } else if (this.state.showEditRole) {
      content =
        (<EditRole initialRole={this.state.editRole}
                   streams={this.state.streams}
                   dashboards={this.state.dashboards.dashboards}
                   onSave={this._saveRole}
                   cancelEdit={this._clearEditRole} />);
    } else {
      content = (<RoleList roles={this.state.roles}
                           showEditRole={this._showEditRole}
                           deleteRole={this._deleteRole} />);
    }

    let actionButton;
    if (!this.state.showEditRole) {
      actionButton = <Button bsStyle="success" onClick={this._showCreateRole}>Add new role</Button>;
    }
    return (
      <Row>
        <Col md={12}>
          <PageHeader title="Roles" subpage>
            <span>
              Roles bundle permissions which can be assigned to multiple users at once
            </span>
            {null}
            <span>
              {actionButton}
            </span>
          </PageHeader>

          {content}
        </Col>
      </Row>
    );
  },
});
