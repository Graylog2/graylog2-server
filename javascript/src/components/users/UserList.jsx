'use strict';

var React = require('react');

var UsersStore = require('../../stores/users/UsersStore');
var DataTable = require('../common/DataTable');

var Permissions = require('../../logic/permissions/Permissions');

var UserList = React.createClass({
    getInitialState() {
        return {
            currentUsername: this.props.currentUsername,
            currentUser: null,
            permissions: null,
            users: []
        };
    },
    componentDidMount() {
        var promise = UsersStore.loadUsers();
        promise.done((users) => {
            var currentUser = users.filter((user) => user.username === this.state.currentUsername)[0];
            this.setState({
                currentUser: currentUser,
                permissions: new Permissions(currentUser.permissions),
                users: users
            });
        });
    },
    _hasAdminRole(user) {
        return user.permissions.some((permission) => permission === "*");
    },
    deleteUser(username) {
        var promise = UsersStore.deleteUser(username);

        promise.done(() => {
            var users = this.state.users.filter((user) => user.username !== username);
            this.setState({
                users: users
            });
        });
    },
    _deleteUserFunction(username) {
        return () => {
            if (window.confirm("Do you really want to delete user " + username + "?")) {
                this.deleteUser(username);
            }
        };
    },
    render() {
        var users = this.state.users.map((user) => {
            var rowClass = user.username === this.state.currentUsername ? "info" : null;
            var userBadge = null;
            if (user.read_only) {
                userBadge = <span><i title="System User" className="fa fa-lock"></i></span>;
            }
            if (user.external) {
                userBadge = <span><i title="LDAP User" className="fa fa-cloud"></i></span>;
            }

            var roleBadge = null;
            if (this._hasAdminRole(user)) {
                roleBadge = <span className="label label-info">Admin</span>;
            } else {
                roleBadge = <span className="label label-default">Reader</span>;
            }

            var actions = null;
            if (!user.read_only) {
                var deleteAction = (
                    <button id="delete-user" type="button" className="btn btn-xs btn-danger" title="Delete user"
                            onClick={this._deleteUserFunction(user.username)}>
                        <i className="fa fa-remove"></i> Delete
                    </button>
                );

                var editAction = (
                    <a id="edit-user" href={UsersStore.editUserFormUrl(user.username)}
                       className="btn btn-default btn-xs" title={"Edit user " + user.username}>
                        <i className="fa fa-edit"></i> Edit</a>
                );

                actions = (
                    <div>
                        {this.state.permissions.isPermitted(["users:edit"]) ? deleteAction : null}
                        &nbsp;
                        {editAction}
                    </div>
                );
            }

            return (
                <tr key={user.username} className={rowClass}>
                    <td className="centered">{userBadge}</td>
                    <td className="limited">{user.full_name}</td>
                    <td className="limited">{user.username}</td>
                    <td className="limited">{user.email}</td>
                    <td>{roleBadge}</td>
                    <td>{actions}</td>
                </tr>
            );
        });

        var headers = (
            <tr>
                <th></th>
                <th className="name">Name</th>
                <th>Username</th>
                <th>Email Address</th>
                <th>Role</th>
                <th className="actions">Actions</th>
            </tr>
        );

        return (
            <DataTable id="user-list" headers={headers} rows={users}/>
        );
    }
});

module.exports = UserList;