'use strict';

var React = require('react');
var Immutable = require('immutable');

var Button = require('react-bootstrap').Button;

var DataTable = require('../common/DataTable');
var PermissionsMixin = require('../../util/PermissionsMixin');

var RoleList = React.createClass({
    mixins: [PermissionsMixin],

    propTypes: {
        roles: React.PropTypes.instanceOf(Immutable.Set).isRequired,
        showEditRole: React.PropTypes.func.isRequired,
        deleteRole: React.PropTypes.func.isRequired,
    },

    _headerCellFormatter(header) {
        const className = (header === 'Actions' ? 'actions' : '');
        return <th className={className}>{header}</th>;
    },
    _roleInfoFormatter(role) {

        let actions = [
            <button key="delete" className="btn btn-primary btn-xs" onClick={() => this.props.deleteRole(role)} title="Delete role">Delete</button>,
            <span key="space">&nbsp;</span>,
            <button key="edit" className="btn btn-info btn-xs" onClick={() => this.props.showEditRole(role)} title="Edit role">Edit</button>
        ];

        return (
            <tr key={role.name}>
                <td>{role.name}</td>
                <td className="limited">{role.description}</td>
                <td>
                    {role.read_only ? null : actions}
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
