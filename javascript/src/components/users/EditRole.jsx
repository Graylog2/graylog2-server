'use strict';

var React = require('react');
var Immutable = require('immutable');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Button = require('react-bootstrap').Button;
var Input = require('react-bootstrap').Input;

var PermissionSelector = require('./PermissionSelector');
var RolesStore = require('../../stores/users/RolesStore').RolesStore;
var PermissionsMixin = require('../../util/PermissionsMixin');

var EditRole = React.createClass({
  mixins: [PermissionsMixin],

  propTypes: {
    initialRole: React.PropTypes.object,
    onSave: React.PropTypes.func.isRequired,
    cancelEdit: React.PropTypes.func.isRequired,
    streams: React.PropTypes.object,
    dashboards: React.PropTypes.object,
  },

  getInitialState() {
    let role = this.props.initialRole;
    if (role === null) {
      // for the create dialog
      role = {name: null, description: null, permissions: []};
    }
    return {
      role: role,
      initialName: this._safeRoleName(this.props.initialRole)
    };
  },

  _safeRoleName(role) {
    return role === null ? null : role.name;
  },

  componentWillReceiveProps(newProps) {
    this.setState({role: newProps.initialRole, initialName: this._safeRoleName(newProps.initialRole)});
  },

  _setName(ev) {
    const role = this.state.role;
    role.name = ev.target.value;
    this.setState({role: this.state.role});
  },
  _setDescription(ev) {
    const role = this.state.role;
    role.description = ev.target.value;
    this.setState({role: this.state.role});
  },

  _updatePermissions(addedPerms, deletedPerms) {
    const role = this.state.role;
    role.permissions = Immutable.Set(role.permissions).subtract(deletedPerms).union(addedPerms).toJS();
    this.setState({role: role});
  },

  render() {
    let titleText;
    if (this.state.initialName === null) {
      titleText = 'Create a new role';
    } else {
      titleText = 'Edit role ' + this.state.initialName;
    }
    return (
      <Row>
        <Col md={12}>
          <h1>{titleText}</h1>
          <div>
            <Input id="role-name" type="text" label="Name" onChange={this._setName} value={this.state.role.name}
                   required/>
            <Input id="role-description" type="text" label="Description" onChange={this._setDescription}
                   value={this.state.role.description} required/>

            <PermissionSelector streams={this.props.streams}
                                dashboards={this.props.dashboards}
                                permissions={Immutable.Set(this.state.role.permissions)}
                                onChange={this._updatePermissions}
            />

            <Button onClick={ev => this.props.onSave(this.state.initialName, this.state.role)}>Save</Button>
            <Button onClick={this.props.cancelEdit}>Cancel</Button>
          </div>
        </Col>
      </Row>);
  },
});

module.exports = EditRole;
