import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import { Button, Col, Row } from 'components/graylog';
import UserNotification from 'util/UserNotification';
import RoleList from 'components/users/RoleList';
import EditRole from 'components/users/EditRole';
import PageHeader from 'components/common/PageHeader';

import CombinedProvider from 'injection/CombinedProvider';
import { DashboardsActions, DashboardsStore } from 'views/stores/DashboardsStore';
import connect from 'stores/connect';

const { StreamsStore } = CombinedProvider.get('Streams');
const { RolesStore } = CombinedProvider.get('Roles');

class RolesComponent extends React.Component {
  static propTypes = {
    dashboards: PropTypes.shape({
      list: PropTypes.array,
    }).isRequired,
  };

  state = {
    roles: Immutable.Set(),
    rolesLoaded: false,
    editRole: null,
    streams: Immutable.List(),
  };

  componentDidMount() {
    this.loadRoles();
    StreamsStore.load(streams => this.setState({ streams: Immutable.List(streams) }));
    DashboardsActions.search('', 1, 32768);
  }

  loadRoles = () => {
    const promise = RolesStore.loadRoles();
    promise.then((roles) => {
      this.setState({ roles: Immutable.Set(roles), rolesLoaded: true });
    });
  };

  _showCreateRole = () => {
    this.setState({ showEditRole: true });
  };

  _showEditRole = (role) => {
    this.setState({ showEditRole: true, editRole: role });
  };

  _deleteRole = (role) => {
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
  };

  _saveRole = (initialName, role) => {
    if (initialName === null) {
      RolesStore.createRole(role).then(this._clearEditRole).then(this.loadRoles);
    } else {
      RolesStore.updateRole(initialName, role).then(this._clearEditRole).then(this.loadRoles);
    }
  };

  _clearEditRole = () => {
    this.setState({ showEditRole: false, editRole: null });
  };

  render() {
    let content = null;
    const { rolesLoaded, showEditRole, editRole, streams, roles } = this.state;
    if (!rolesLoaded) {
      content = <span>Loading roles...</span>;
    } else if (showEditRole) {
      const { dashboards } = this.props;
      content = (
        <EditRole initialRole={editRole}
                  streams={streams}
                  dashboards={dashboards.list}
                  onSave={this._saveRole}
                  cancelEdit={this._clearEditRole} />
      );
    } else {
      content = (
        <RoleList roles={roles}
                  showEditRole={this._showEditRole}
                  deleteRole={this._deleteRole} />
      );
    }

    let actionButton;
    if (!showEditRole) {
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
  }
}

export default connect(RolesComponent, { dashboards: DashboardsStore });
