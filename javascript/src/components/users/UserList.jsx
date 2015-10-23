import React from 'react';
import Reflux from 'reflux';

import PermissionsMixin from 'util/PermissionsMixin';

import UsersStore from 'stores/users/UsersStore';
import RolesStore from 'stores/users/RolesStore';

import DataTable from 'components/common/DataTable';
import Spinner from 'components/common/Spinner';

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
    RolesStore.loadRoles().done((response) => {
      const roles = response.roles;
      this.setState({roles: roles.map(role => role.name)});
    });
  },
  loadUsers() {
    const promise = UsersStore.loadUsers();
    promise.done((response) => {
      const users = response.users;
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
      if (window.confirm('Do you really want to delete user ' + username + '?')) {
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
    if (user.read_only) {
      userBadge = <span><i title="System User" className="fa fa-lock"/></span>;
    }
    if (user.external) {
      userBadge = <span><i title="LDAP User" className="fa fa-cloud"/></span>;
    }

    const roleBadges = user.roles.map((role) => <span key={role} className={`label label-${role === 'Admin' ? 'info' : 'default'}`} style={{marginRight: 5}}>{role}</span>);

    let actions = null;
    if (!user.read_only) {
      const deleteAction = (
        <button id="delete-user" type="button" className="btn btn-xs btn-primary" title="Delete user"
                onClick={this._deleteUserFunction(user.username)}>
          Delete
        </button>
      );

      const editAction = (
        <a id="edit-user" href={UsersStore.editUserFormUrl(user.username)}
           className="btn btn-info btn-xs" title={'Edit user ' + user.username}>
          Edit
        </a>
      );

      actions = (
        <div>
          {this.isPermitted(this.props.currentUser.permissions, ['users:edit']) ? deleteAction : null}
          &nbsp;
          {this.isPermitted(this.props.currentUser.permissions, ['users:edit:' + user.username]) ? editAction : null}
        </div>
      );
    }

    return (
      <tr key={user.username} className={rowClass}>
        <td className="centered">{userBadge}</td>
        <td className="limited">{user.full_name}</td>
        <td className="limited">{user.username}</td>
        <td className="limited">{user.email}</td>
        <td>{roleBadges}</td>
        <td>{actions}</td>
      </tr>
    );
  },
  render() {
    const filterKeys = ['username', 'full_name', 'email'];
    const headers = ['', 'Name', 'Username', 'Email Address', 'Role', 'Actions'];

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
