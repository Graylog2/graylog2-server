/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

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
import AppConfig from 'util/AppConfig';

import TimezoneFormGroup from './TimezoneFormGroup';
import TimeoutFormGroup from './TimeoutFormGroup';
import FullNameFormGroup from './FullNameFormGroup';
import EmailFormGroup from './EmailFormGroup';
import PasswordFormGroup, { validatePasswords } from './PasswordFormGroup';
import UsernameFormGroup from './UsernameFormGroup';

import { Headline } from '../../common/Section/SectionComponent';

const isCloud = AppConfig.isCloud();
const cloudPlugin = isCloud ? PluginStore.exports('cloud') : null;

const _onSubmit = (formData, roles, setSubmitError) => {
  let data = { ...formData, roles: roles.toJS(), permissions: [] };
  delete data.password_repeat;

  if (isCloud && cloudPlugin) {
    const onCreate = cloudPlugin?.[0]?.oktaUserForm.onCreate;
    data = onCreate(data);
  } else {
    data.username = data.username.trim();
  }

  setSubmitError(null);

  return UsersDomain.create(data).then(() => {
    history.push(Routes.SYSTEM.USERS.OVERVIEW);
  }, (error) => setSubmitError(error));
};

const _validate = (values) => {
  let errors = {};

  const { password, password_repeat: passwordRepeat } = values;

  if (isCloud && cloudPlugin) {
    const validateCloudPasswords = cloudPlugin?.[0]?.oktaUserForm.validations.password;

    errors = validateCloudPasswords(errors, password, passwordRepeat);
  } else {
    errors = validatePasswords(errors, password, passwordRepeat);
  }

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

  const getUserNameEmailGroup = () => {
    if (isCloud && cloudPlugin) {
      const CloudUserNameFormGroup = cloudPlugin?.[0]?.oktaUserForm.fields.username;
      const CloudEmailFormGroup = cloudPlugin?.[0]?.oktaUserForm.fields.email;

      return (
        <>
          {CloudUserNameFormGroup && <CloudUserNameFormGroup />}
          {CloudEmailFormGroup && <CloudEmailFormGroup /> }
        </>
      );
    }

    return (
      <>
        <UsernameFormGroup users={users} />
        <EmailFormGroup />
      </>
    );
  };

  const getPasswordGroup = () => {
    if (isCloud && cloudPlugin) {
      const CloudPasswordFormGroup = cloudPlugin?.[0]?.oktaUserForm.fields.password;

      return <CloudPasswordFormGroup />;
    }

    return <PasswordFormGroup />;
  };

  if (!users) {
    return <Spinner />;
  }

  const showSubmitError = (errors) => {
    if (isCloud && cloudPlugin) {
      const extractSubmitError = cloudPlugin?.[0]?.oktaUserForm.extractSubmitError;

      return extractSubmitError(errors);
    }

    return errors?.additional?.res?.text;
  };

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
                <FullNameFormGroup />
                {getUserNameEmailGroup()}
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
                {getPasswordGroup()}
              </div>
              {submitError && (
                <Row>
                  <Col xs={9} xsOffset={3}>
                    <Alert bsStyle="danger">
                      <b>Failed to create user</b><br />
                      {showSubmitError(submitError)}
                    </Alert>
                  </Col>
                </Row>
              )}
              <Row>
                <Col md={9} mdOffset={3}>
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
