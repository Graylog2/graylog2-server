'use strict';

var React = require('react');
var Immutable = require('immutable');

var DataTable = require('../common/DataTable');
var PermissionsMixin = require('../../util/PermissionsMixin');

var RoleList = React.createClass({
    mixins: [PermissionsMixin],

    propTypes: {
        roles: React.PropTypes.instanceOf(Immutable.Set).isRequired,
        showEditRole: React.PropTypes.func.isRequired,
        deleteRole: React.PropTypes.func.isRequired
    },

    _headerCellFormatter(header) {
        return <th>{header}</th>;
    },
    _roleInfoFormatter(role) {
        return (
            <tr key={role.name}>
                <td className="centered">{role.name}</td>
                <td className="limited">{role.description}</td>
                <td>
                    <button className="btn btn-primary btn-xs" onClick={() => this.props.deleteRole(role)} title="Delete role">Delete</button>
                    <button className="btn btn-info btn-xs" onClick={() => this.props.showEditRole(role)} title="Edit role">Edit</button>
                </td>
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
                           rows={this.props.roles.toJS()}
                           filterBy="Name"
                           dataRowFormatter={this._roleInfoFormatter}
                           filterLabel="Filter Roles"
                           filterKeys={filterKeys}/>
            </div>
        );
    }
});

module.exports = RoleList;