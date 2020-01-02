import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';

import { Button, Alert, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Input } from 'components/bootstrap';
import PermissionSelector from 'components/users/PermissionSelector';

class EditRole extends React.Component {
  constructor(props) {
    super(props);
    const { initialRole } = this.props;
    const role = initialRole;
    this.state = {
      role,
      initialName: this._safeRoleName(initialRole),
    };
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    const {
      role = {
        name: null,
        description: null,
        permissions: [],
      },
    } = prevState;
    return { role: role };
  }

  _safeRoleName = (role) => {
    return role === null ? null : role.name;
  }

  _setName = (ev) => {
    const { role } = this.state;
    role.name = ev.target.value;
    this.setState({ role });
  }

  _setDescription = (ev) => {
    const { role } = this.state;
    role.description = ev.target.value;
    this.setState({ role });
  }

  _updatePermissions = (addedPerms, deletedPerms) => {
    const { role } = this.state;
    role.permissions = Immutable.Set(role.permissions)
      .subtract(deletedPerms)
      .union(addedPerms)
      .toJS();
    this.setState({ role });
  }

  _saveDisabled = () => {
    const { role } = this.state;

    return role === null || role.name === null || role.name === '' || role.permissions.length === 0;
  }

  _onSave = () => {
    const { onSave } = this.props;
    const { initialName, role } = this.state;

    onSave(initialName, role);
  }

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
  }
}

EditRole.propTypes = {
  initialRole: PropTypes.object,
  onSave: PropTypes.func.isRequired,
  cancelEdit: PropTypes.func.isRequired,
  streams: PropTypes.object.isRequired,
  dashboards: PropTypes.object.isRequired,
};
EditRole.defaultProps = {
  initialRole: {
    name: null,
    description: null,
    permissions: [],
  },
};

export default EditRole;
