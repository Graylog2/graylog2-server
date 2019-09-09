import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import { Alert, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import PermissionSelector from 'components/users/PermissionSelector';
import PermissionsMixin from 'util/PermissionsMixin';

const EditRole = createReactClass({
  displayName: 'EditRole',

  propTypes: {
    initialRole: PropTypes.object.isRequired,
    onSave: PropTypes.func.isRequired,
    cancelEdit: PropTypes.func.isRequired,
    streams: PropTypes.object.isRequired,
    dashboards: PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    const { initialRole } = this.props;

    let role = initialRole;
    if (role === null) {
      // for the create dialog
      role = {
        name: null,
        description: null,
        permissions: [],
      };
    }
    return {
      role,
      initialName: this._safeRoleName(initialRole),
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ role: newProps.initialRole, initialName: this._safeRoleName(newProps.initialRole) });
  },

  _safeRoleName(role) {
    return role === null ? null : role.name;
  },

  _setName(ev) {
    const { role } = this.state;
    role.name = ev.target.value;
    this.setState({ role });
  },

  _setDescription(ev) {
    const { role } = this.state;
    role.description = ev.target.value;
    this.setState({ role });
  },

  _updatePermissions(addedPerms, deletedPerms) {
    const { role } = this.state;
    role.permissions = Immutable.Set(role.permissions)
      .subtract(deletedPerms)
      .union(addedPerms)
      .toJS();
    this.setState({ role });
  },

  _saveDisabled() {
    const { role } = this.state;

    return role === null || role.name === null || role.name === '' || role.permissions.length === 0;
  },

  _onSave() {
    const { onSave } = this.props;
    const { initialName, role } = this.state;

    onSave(initialName, role);
  },

  render() {
    const { initialName, role } = this.state;
    const { streams, dashboards, cancelEdit } = this.props;

    let titleText;
    if (initialName === null) {
      titleText = 'Create a new role';
    } else {
      titleText = `Edit role ${initialName}`;
    }

    const saveDisabled = this._saveDisabled();
    let saveDisabledAlert = null;
    if (saveDisabled) {
      saveDisabledAlert = (
        <Alert bsStyle="warning" style={{ marginBottom: 10 }}>
          Please name the role and select at least one permission to save it.
        </Alert>
      );
    }

    return (
      <Row>
        <Col md={12}>
          <h1>{titleText}</h1>
          <div style={{ marginTop: 10 }}>
            <Input id="role-name"
                   type="text"
                   label="Name"
                   onChange={this._setName}
                   value={role.name}
                   required />
            <Input id="role-description"
                   type="text"
                   label="Description"
                   onChange={this._setDescription}
                   value={role.description} />

            <FormGroup>
              <ControlLabel>Permissions</ControlLabel>
              <HelpBlock>Select the permissions for this role</HelpBlock>
            </FormGroup>
            <PermissionSelector streams={streams}
                                dashboards={dashboards}
                                permissions={Immutable.Set(role.permissions)}
                                onChange={this._updatePermissions} />
            <hr />
            {saveDisabledAlert}
            <Button onClick={this._onSave} style={{ marginRight: 5 }} bsStyle="primary" disabled={saveDisabled}>
              Save
            </Button>
            <Button onClick={cancelEdit}>Cancel</Button>
          </div>
        </Col>
      </Row>
    );
  },
});

export default EditRole;
