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
        this.loadUsers();
    },
    loadUsers: function () {
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
            this.loadUsers();
        });
    },
    _deleteUserFunction(username) {
        return () => {
            if (window.confirm("Do you really want to delete user " + username + "?")) {
                this.deleteUser(username);
            }
        };
    },
    _headerCellFormatter(header) {
        var formattedHeaderCell;

        switch (header.toLocaleLowerCase()) {
            case 'name':
                formattedHeaderCell = <th className="name">{header}</th>;
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
    },
    render() {
        var filterKeys = ["username", "full_name", "email"];
        var headers = ["", "Name", "Username", "Email Address", "Role", "Actions"];

        return (
            <div>
                <DataTable id="user-list"
                           headers={headers}
                           headerCellFormatter={this._headerCellFormatter}
                           sortByKey={"full_name"}
                           rows={this.state.users}
                           dataRowFormatter={this._userInfoFormatter}
                           filterLabel="Filter Users"
                           filterKeys={filterKeys}/>
            </div>
        );
    }
});

module.exports = UserList;