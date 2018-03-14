import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { Button } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';

import PermissionsMixin from 'util/PermissionsMixin';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { DataTable } from 'components/common';

const RoleList = createReactClass({
  displayName: 'RoleList',
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  propTypes: {
    roles: ImmutablePropTypes.set.isRequired,
    showEditRole: PropTypes.func.isRequired,
    deleteRole: PropTypes.func.isRequired,
  },

  _headerCellFormatter(header) {
    const className = (header === 'Actions' ? 'actions' : '');
    return <th className={className}>{header}</th>;
  },

  _editButton(role) {
    if (this.isPermitted(this.state.currentUser.permissions, ['roles:edit:' + role.name]) === false || role.read_only) {
        return null;
    }
    return (<Button key="edit" bsSize="xsmall" bsStyle="info" onClick={() => this.props.showEditRole(role)} title="Edit role">Edit</Button>);
  },

  _deleteButton(role) {
    if (this.isPermitted(this.state.currentUser.permissions, ['roles:delete:' + role.name]) === false || role.read_only) {
        return null;
    }
    return (<Button key="delete" bsSize="xsmall" bsStyle="primary" onClick={() => this.props.deleteRole(role)} title="Delete role">Delete</Button>);
  },

  _roleInfoFormatter(role) {
    return (
      <tr key={role.name}>
        <td>{role.name}</td>
        <td className="limited">{role.description}</td>
        <td>
          {this._editButton(role)}
          <span key="space">&nbsp;</span>
          {this._deleteButton(role)}
        </td>
      </tr>
    );
  },

  render() {
    const filterKeys = ['name', 'description'];
    const headers = ['Name', 'Description', 'Actions'];

    return (
      <div>
        <DataTable id="role-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey={'name'}
                   rows={this.props.roles.toJS()}
                   filterBy="Name"
                   dataRowFormatter={this._roleInfoFormatter}
                   filterLabel="Filter Roles"
                   filterKeys={filterKeys} />
      </div>
    );
  },
});

export default RoleList;
