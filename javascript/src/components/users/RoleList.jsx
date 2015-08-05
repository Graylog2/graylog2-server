'use strict';

var React = require('react');
var Immutable = require('immutable');

var RolesStore = require('../../stores/users/RolesStore');
var DataTable = require('../common/DataTable');
var PermissionsMixin = require('../../util/PermissionsMixin');

var RoleList = React.createClass({
    mixins: [PermissionsMixin],

    getInitialState() {
        return {
            roles: Immutable.Set()
        };
    },

    componentDidMount() {
        this.loadRoles();
    },

    loadRoles() {
        var promise = RolesStore.loadRoles();
        promise.done((roles) => {
            this.setState({roles: Immutable.Set(roles)});
        });
    },

    _headerCellFormatter(header) {
        return <th>{header}</th>;
    },
    _roleInfoFormatter(role) {
        return (
            <tr key={role.name}>
                <td className="centered">{role.name}</td>
                <td className="limited">{role.description}</td>
                <td>Actions</td>
            </tr>
        );
    },
    render() {
        var filterKeys = ["name", "description"];
        var headers = ["Name", "Description", "Actions"];

        return (
            <div>
                <DataTable id="role-list"
                           className="table-hover"
                           headers={headers}
                           headerCellFormatter={this._headerCellFormatter}
                           sortByKey={"name"}
                           rows={this.state.roles.toJS()}
                           filterBy="Name"
                           dataRowFormatter={this._roleInfoFormatter}
                           filterLabel="Filter Roles"
                           filterKeys={filterKeys}/>
            </div>
        );
    }
});

module.exports = RoleList;