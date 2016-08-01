import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Label, OverlayTrigger, Popover, Tooltip } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const UsersStore = StoreProvider.getStore('Users');
const RolesStore = StoreProvider.getStore('Roles');

import { DataTable, Spinner, Timestamp } from 'components/common';

import UserListStyle from '!style!css!./UserList.css';

const UserList = React.createClass({
  propTypes: {
    currentUsername: React.PropTypes.string.isRequired,
    currentUser: React.PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    return {
      users: undefined,
      roles: undefined,
    };
  },
  componentDidMount() {
    this.loadUsers();
    RolesStore.loadRoles().done(roles => {
      this.setState({ roles: roles.map(role => role.name) });
    });
  },

  loadUsers() {
    const promise = UsersStore.loadUsers();
    promise.done((users) => {
      this.setState({
        users: users,
      });
    });
  },
  _hasAdminRole(user) {
    return this.isPermitted(user.permissions, ['*']);
  },
  deleteUser(username) {
    const promise = UsersStore.deleteUser(username);

    promise.done(() => {
      this.loadUsers();
    });
  },
  _deleteUserFunction(username) {
    return () => {
      if (window.confirm(`Do you really want to delete user ${username}?`)) {
        this.deleteUser(username);
      }
    };
  },
  _headerCellFormatter(header) {
    let formattedHeaderCell;

    switch (header.toLocaleLowerCase()) {
      case '':
        formattedHeaderCell = <th className="user-type">{header}</th>;
        break;
      case 'actions':
        formattedHeaderCell = <th className="actions">{header}</th>;
        break;
      default:
        formattedHeaderCell = <th>{header}</th>;
    }

    return formattedHeaderCell;
  },
  _userInfoFormatter(user) {
    const rowClass = user.username === this.props.currentUsername ? 'active' : null;
    let userBadge = null;
    if (user.session_active) {
      const popover = (
        <Popover id="session-badge-details" title="Logged in" className={UserListStyle.sessionBadgeDetails}>
          <div>Last activity: <Timestamp dateTime={user.last_activity} relative /></div>
          <div>Client address: {user.client_address}</div>
        </Popover>
      );
      userBadge = (<OverlayTrigger trigger={['hover', 'focus']} placement="left" overlay={popover} rootClose>
        <i className={`fa fa-circle ${UserListStyle.activeSession}`}/>
      </OverlayTrigger>);
    }

    const roleBadges = user.roles.map((role) => <span key={role} className={`${UserListStyle.roleBadgeFixes} label label-${role === 'Admin' ? 'info' : 'default'}`} >{role}</span>);

    let actions = null;
    if (user.read_only) {
      const tooltip = <Tooltip id="system-user">System users can only be modified in the Graylog configuration file.</Tooltip>;
      actions = (
        <OverlayTrigger placement="left" overlay={tooltip}>
          <span className={UserListStyle.help}>
            <Button bsSize="xs" bsStyle="info" disabled>System user</Button>
          </span>
        </OverlayTrigger>
      );
    } else {
      const deleteAction = (
        <Button id="delete-user" bsStyle="primary" bsSize="xs" title="Delete user"
                onClick={this._deleteUserFunction(user.username)}>
          Delete
        </Button>
      );

      const editAction = (
        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(user.username)}>
          <Button bsStyle="info" bsSize="xs" title={`Edit user ${user.username}`}>
            Edit
          </Button>
        </LinkContainer>
      );

      actions = (
        <div>
          {this.isPermitted(this.props.currentUser.permissions, ['users:edit']) ? deleteAction : null}
          &nbsp;
          {this.isPermitted(this.props.currentUser.permissions, [`users:edit:${user.username}`]) ? editAction : null}
        </div>
      );
    }

    return (
      <tr key={user.username} className={rowClass}>
        <td className="centered">{userBadge}</td>
        <td className="limited">{user.full_name}</td>
        <td className="limited">{user.username}</td>
        <td className="limited">{user.email}</td>
        <td className="limited">{user.client_address}</td>
        <td className={UserListStyle.limitedWide}>{roleBadges}</td>
        <td>{actions}</td>
      </tr>
    );
  },
  render() {
    const filterKeys = ['username', 'full_name', 'email', 'client_address'];
    const headers = ['', 'Name', 'Username', 'Email Address', 'Client Address', 'Role', 'Actions'];

    if (this.state.users && this.state.roles) {
      return (
        <div>
          <DataTable id="user-list"
                     className="table-hover"
                     headers={headers}
                     headerCellFormatter={this._headerCellFormatter}
                     sortByKey={"full_name"}
                     rows={this.state.users}
                     filterBy="role"
                     filterSuggestions={this.state.roles}
                     dataRowFormatter={this._userInfoFormatter}
                     filterLabel="Filter Users"
                     filterKeys={filterKeys}/>
        </div>
      );
    }

    return <Spinner />;
  },
});

export default UserList;
