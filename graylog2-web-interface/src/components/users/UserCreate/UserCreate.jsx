// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';

import type { DescriptiveItem } from 'components/common/PaginatedItemOverview';
import User from 'logic/users/User';
import UsersDomain from 'domainActions/users/UsersDomain';
import PaginatedItem from 'components/common/PaginatedItemOverview/PaginatedItem';
import RolesSelector from 'components/permissions/RolesSelector';
import { Alert, Col, Row, Button, ButtonToolbar } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import history from 'util/History';
import Routes from 'routing/Routes';

import TimezoneFormGroup from './TimezoneFormGroup';
import TimeoutFormGroup from './TimeoutFormGroup';
import FullNameFormGroup from './FullNameFormGroup';
import EmailFormGroup from './EmailFormGroup';
import PasswordFormGroup, { validatePasswords } from './PasswordFormGroup';
import UsernameFormGroup from './UsernameFormGroup';

import { Headline } from '../../common/Section/SectionComponent';

const _onSubmit = (formData, roles, setSubmitError) => {
  const data = { ...formData, roles: roles.toJS(), permissions: [] };
  delete data.password_repeat;

  setSubmitError(null);

  UsersDomain.create(data).then(() => {
    history.push(Routes.SYSTEM.USERS.OVERVIEW);
  }, (error) => setSubmitError(error));
};

const _validate = (values) => {
  let errors = {};
  const { password, password_repeat: passwordRepeat } = values;
  errors = validatePasswords(errors, password, passwordRepeat);

  return errors;
};

const UserCreate = () => {
  const initialRole = { name: 'Reader', description: 'Grants basic permissions for every Graylog user (built-in)', id: '' };
  const [users, setUsers] = useState();
  const [user, setUser] = useState(User.empty().toBuilder().roles(Immutable.Set([initialRole.name])).build());
  const [submitError, setSubmitError] = useState();
  const [selectedRoles, setSelectedRoles] = useState<Immutable.Set<DescriptiveItem>>(Immutable.Set([initialRole]));

  useEffect(() => {
    UsersDomain.loadUsers().then(setUsers);
  }, []);

  const _onAssignRole = (roles: Immutable.Set<DescriptiveItem>) => {
    setSelectedRoles(selectedRoles.union(roles));
    const roleNames = roles.map((r) => r.name);

    return Promise.resolve(
      setUser(user.toBuilder().roles(user.roles.union(roleNames)).build()),
    );
  };

  const _onUnassignRole = (role: DescriptiveItem) => {
    setSelectedRoles(selectedRoles.remove(role));
    setUser(user.toBuilder().roles(user.roles.remove(role?.name)).build());
  };

  const _handleCancel = () => history.push(Routes.SYSTEM.USERS.OVERVIEW);
  const hasValidRole = selectedRoles.size > 0 && selectedRoles.filter((role) => role.name === 'Reader' || role.name === 'Admin');

  if (!users) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col lg={8}>
        <Formik onSubmit={(data) => _onSubmit(data, user.roles, setSubmitError)}
                validate={_validate}
                initialValues={{}}>
          {({ isSubmitting, isValid }) => (
            <Form className="form form-horizontal">
              <div>
                <Headline>Profile</Headline>
                <UsernameFormGroup users={users} />
                <FullNameFormGroup />
                <EmailFormGroup />
              </div>
              <div>
                <Headline>Settings</Headline>
                <TimeoutFormGroup />
                <TimezoneFormGroup />
              </div>
              <div>
                <Headline>Roles</Headline>
                <Input id="roles-selector-input"
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Assign Roles">
                  { /* $FlowFixMe: assignRole has DescriptiveItem */}
                  <RolesSelector onSubmit={_onAssignRole} assignedRolesIds={user.roles} identifier={(role) => role.name} />
                </Input>

                <Input id="selected-roles-overview"
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Selected Roles">
                  <>
                    {selectedRoles.map((role) => (
                      <PaginatedItem item={role}
                                     onDeleteItem={(data) => _onUnassignRole(data)}
                                     key={role.id} />
                    ))}
                    {!hasValidRole && <Alert bsStyle="danger">You need to select at least one of the <em>Reader</em> or <em>Admin</em> roles.</Alert>}
                  </>
                </Input>
              </div>
              <div>
                <Headline>Password</Headline>
                <PasswordFormGroup />
              </div>
              {submitError && (
                <Row>
                  <Col xs={9} xsOffset={3}>
                    <Alert bsStyle="danger">
                      <b>Failed to create user</b><br />
                      {submitError?.additional?.res?.text}
                    </Alert>
                  </Col>
                </Row>
              )}
              <Row>
                <Col xs={9} xsOffset={3}>
                  <ButtonToolbar>
                    <Button bsStyle="success"
                            disabled={isSubmitting || !isValid || !hasValidRole}
                            title="Create User"
                            type="submit">
                      Create User
                    </Button>
                    <Button type="button" onClick={_handleCancel}>Cancel</Button>
                  </ButtonToolbar>
                </Col>
              </Row>
            </Form>
          )}
        </Formik>
      </Col>
    </Row>
  );
};

export default UserCreate;
