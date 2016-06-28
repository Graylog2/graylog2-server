import React from 'react';
import { Alert, Col, Button, Input, Row } from 'react-bootstrap';
import Routes from 'routing/Routes';

import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const RolesStore = StoreProvider.getStore('Roles');
const UsersStore = StoreProvider.getStore('Users');

import RolesSelect from 'components/users/RolesSelect';
import { Spinner } from 'components/common';

const EditRolesForm = React.createClass({
  propTypes: {
    user: React.PropTypes.object.isRequired,
    history: React.PropTypes.object,
  },
  getInitialState() {
    return {};
  },
  componentDidMount() {
    RolesStore.loadRoles().then(roles => {
      this.setState({ roles: roles.sort((r1, r2) => r1.name.localeCompare(r2.name)) });
    });
  },
  _updateRoles(evt) {
    evt.preventDefault();
    if (confirm(`Really update roles for "${this.props.user.username}"?`)) {
      const roles = this.refs.roles.getValue().filter((value) => value !== '');
      UsersStore.updateRoles(this.props.user.username, roles).then(() => {
        UserNotification.success('Roles updated successfully.', 'Success!');
        this.props.history.replaceState(null, Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
      }, () => {
        UserNotification.error('Updating roles failed.', 'Error!');
      });
    }
  },
  render() {
    const user = this.props.user;
    if (!this.state.roles) {
      return <Spinner />;
    }
    const externalUser = user.external ?
      (
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
          You cannot edit the admin's user role.
        </Alert>
      </Col>
    ) : (
      <span>
        {externalUser}
        <form className="form-horizontal" style={{ marginTop: '10px' }} onSubmit={this._updateRoles}>
          <Input label="Roles" help="Choose the roles the user should be a member of. All the granted permissions will be combined."
                 labelClassName="col-sm-3" wrapperClassName="col-sm-9">
            <RolesSelect ref="roles" userRoles={user.roles} availableRoles={this.state.roles} />
          </Input>
          <div className="form-group">
            <Col smOffset={3} sm={9}>
              <Button bsStyle="success" type="submit">
                Update role
              </Button>
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
  },
});

export default EditRolesForm;
