import PropTypes from 'prop-types';
import React from 'react';
import Routes from 'routing/Routes';

import { Button, Alert, Col, Row } from 'components/graylog';
import { Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import ObjectUtils from 'util/ObjectUtils';
import history from 'util/History';

import StoreProvider from 'injection/StoreProvider';

import RolesSelect from 'components/users/RolesSelect';
import { Spinner } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import EditRolesFormStyle from '!style!css!./EditRolesForm.css';

const RolesStore = StoreProvider.getStore('Roles');
const UsersStore = StoreProvider.getStore('Users');

class EditRolesForm extends React.Component {
  static propTypes = {
    user: PropTypes.object.isRequired,
  };

  state = {
    newRoles: null,
  };

  componentDidMount() {
    RolesStore.loadRoles().then((roles) => {
      this.setState({ roles: roles.sort((r1, r2) => r1.name.localeCompare(r2.name)) });
    });
  }

  _updateRoles = (evt) => {
    const { user } = this.props;

    evt.preventDefault();
    // eslint-disable-next-line no-alert
    if (window.confirm(`Really update roles for "${user.username}"?`)) {
      const roles = this.roles.getValue().filter((value) => value !== '');
      const userClone = ObjectUtils.clone(user);
      userClone.roles = roles;
      UsersStore.update(user.username, userClone).then(() => {
        UserNotification.success('Roles updated successfully.', 'Success!');
        history.replace(Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
      }, () => {
        UserNotification.error('Updating roles failed.', 'Error!');
      });
    }
  };

  _onCancel = () => {
    history.push(Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
  };

  _onValueChange = (newRoles) => {
    const roles = newRoles.split(',');
    this.setState({ newRoles: roles });
  };

  render() {
    const { user } = this.props;
    const { roles, newRoles } = this.state;
    if (!roles) {
      return <Spinner />;
    }
    let rolesAlert = null;
    if (newRoles != null && !(newRoles.includes('Reader') || newRoles.includes('Admin'))) {
      rolesAlert = (
        <Alert bsStyle="danger" role="alert" className={EditRolesFormStyle.rolesMissingAlert}>
          You need to select at least one of the <em>Reader</em> or <em>Admin</em> roles.
        </Alert>
      );
    }
    const externalUser = user.external
      ? (
        <Col smOffset={3} sm={9} style={{ marginBottom: 15 }}>
          <Alert bsStyle="warning" role="alert">
            This user was created from an external LDAP system, please consider mapping LDAP groups instead of manually editing roles here.
            Please update the LDAP group mapping to make changes or contact an administrator for more information.
          </Alert>
        </Col>
      ) : null;
    const editUserForm = user.read_only ? (
      <Col smOffset={3} sm={9}>
        <Alert bsStyle="warning" role="alert">
          You cannot edit the admin&apos;s user role.
        </Alert>
      </Col>
    ) : (
      <span>
        {externalUser}
        <form className="form-horizontal" style={{ marginTop: '10px' }} onSubmit={this._updateRoles}>
          <Input id="roles-select"
                 label="Roles"
                 help="Choose the roles the user should be a member of. All the granted permissions will be combined."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <RolesSelect ref={(elem) => { this.roles = elem; }}
                         userRoles={user.roles}
                         availableRoles={roles}
                         onValueChange={this._onValueChange} />
          </Input>
          <div className="form-group">
            <Col smOffset={3} sm={9}>
              {rolesAlert}
              <Button bsStyle="primary" type="submit" className="save-button-margin" disabled={!!rolesAlert}>
                Update role
              </Button>
              <Button onClick={this._onCancel}>Cancel</Button>
            </Col>
          </div>
        </form>
      </span>
    );
    return (
      <Row>
        <Col md={8}>
          <h2>Change user role</h2>
          {editUserForm}
        </Col>
      </Row>
    );
  }
}

export default EditRolesForm;
