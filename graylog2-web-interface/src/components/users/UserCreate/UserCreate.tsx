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
import React, { useState } from 'react';
import styled from 'styled-components';
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { useFormikContext } from 'formik';

import AppConfig from 'util/AppConfig';
import type { DescriptiveItem } from 'components/common/PaginatedItemOverview';
import User from 'logic/users/User';
import PaginatedItem from 'components/common/PaginatedItemOverview/PaginatedItem';
import RolesSelector from 'components/permissions/RolesSelector';
import { Alert, Input } from 'components/bootstrap';
import { IfPermitted, NoSearchResult, ReadOnlyFormGroup, Link } from 'components/common';
import Routes from 'routing/Routes';
import useIsGlobalTimeoutEnabled from 'hooks/useIsGlobalTimeoutEnabled';
import { Headline } from 'components/common/Section/SectionComponent';
import usePasswordComplexityConfig from 'components/users/usePasswordComplexityConfig';
import useProductName from 'brand-customization/useProductName';

import TimezoneFormGroup from './TimezoneFormGroup';
import TimeoutFormGroup from './TimeoutFormGroup';
import FirstNameFormGroup from './FirstNameFormGroup';
import LastNameFormGroup from './LastNameFormGroup';
import EmailFormGroup from './EmailFormGroup';
import PasswordFormGroup from './PasswordFormGroup';
import UsernameFormGroup from './UsernameFormGroup';
import ServiceAccountFormGroup from './ServiceAccountFormGroup';

const GlobalTimeoutMessage = styled(ReadOnlyFormGroup)`
  margin-bottom: 20px;

  .read-only-value-col {
    padding-top: 0;
  }
`;

const isCloud = AppConfig.isCloud();
const oktaUserForm = isCloud ? PluginStore.exports('cloud')[0].oktaUserForm : null;

const PasswordGroup = () => {
  const passwordComplexityConfig = usePasswordComplexityConfig();

  if (isCloud && oktaUserForm) {
    const {
      fields: { password: CloudPasswordFormGroup },
    } = oktaUserForm;

    return <CloudPasswordFormGroup />;
  }

  return <PasswordFormGroup passwordComplexityConfig={passwordComplexityConfig} />;
};

const UserNameGroup = () => {
  if (isCloud && oktaUserForm) {
    const {
      fields: { username: CloudUserNameFormGroup },
    } = oktaUserForm;

    return CloudUserNameFormGroup && <CloudUserNameFormGroup />;
  }

  return <UsernameFormGroup />;
};

const EmailGroup = () => {
  if (isCloud && oktaUserForm) {
    const {
      fields: { email: CloudEmailFormGroup },
    } = oktaUserForm;

    return CloudEmailFormGroup && <CloudEmailFormGroup />;
  }

  return <EmailFormGroup />;
};

const UserCreate = () => {
  const productName = useProductName();
  const isGlobalTimeoutEnabled = useIsGlobalTimeoutEnabled();
  const { setFieldValue } = useFormikContext();

  const initialRole = {
    name: 'Reader',
    description: `Grants basic permissions for every ${productName} user (built-in)`,
    id: '',
  };

  const [user, setUser] = useState(
    User.empty()
      .toBuilder()
      .roles(Immutable.Set([initialRole.name]))
      .build(),
  );

  const [selectedRoles, setSelectedRoles] = useState<Immutable.Set<DescriptiveItem>>(Immutable.Set([initialRole]));

  const hasValidRole =
    selectedRoles.size > 0 && selectedRoles.filter((role) => role.name === 'Reader' || role.name === 'Admin');

  const handleAssignRole = (roles: Immutable.Set<DescriptiveItem>) => {
    const roleNames = roles.map((r) => r.name);
    const newRoleNames = user.roles.union(roleNames);

    setSelectedRoles((prev) => prev.union(roles));
    setUser((prev) => prev.toBuilder().roles(newRoleNames).build());
    setFieldValue('roles', newRoleNames.toJS());

    return Promise.resolve();
  };

  const handleUnassignRole = (role: DescriptiveItem) => {
    const newRoleNames = user.roles.remove(role?.name);

    setSelectedRoles((prev) => prev.remove(role));
    setUser((prev) => prev.toBuilder().roles(newRoleNames).build());
    setFieldValue('roles', newRoleNames.toJS());
  };

  return (
    <>
      <div>
        <Headline>Profile</Headline>
        <FirstNameFormGroup />
        <LastNameFormGroup />
        <UserNameGroup />
        <EmailGroup />
      </div>
      <div>
        <Headline>Settings</Headline>
        {isGlobalTimeoutEnabled ? (
          <GlobalTimeoutMessage
            label="Sessions Timeout"
            value={
              <NoSearchResult>
                User session timeout is not editable because the
                <IfPermitted permissions={['clusterconfigentry:read']}>
                  <Link to={Routes.SYSTEM.CONFIGURATIONS}>global session timeout</Link>
                </IfPermitted>{' '}
                is enabled.
              </NoSearchResult>
            }
          />
        ) : (
          <TimeoutFormGroup />
        )}
        <TimezoneFormGroup />
        <ServiceAccountFormGroup />
      </div>
      <div>
        <Headline>Roles</Headline>
        <Input id="roles-selector-input" labelClassName="col-sm-3" wrapperClassName="col-sm-9" label="Assign Roles">
          <RolesSelector
            onSubmit={handleAssignRole}
            assignedRolesIds={user.roles}
            identifier={(role) => role.name}
            submitOnSelect
          />
        </Input>

        <Input
          id="selected-roles-overview"
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          label="Selected Roles">
          <>
            {selectedRoles
              .map((role) => (
                <PaginatedItem item={role} onDeleteItem={(data) => handleUnassignRole(data)} key={role.id} />
              ))
              .toArray()}
            {!hasValidRole && (
              <Alert bsStyle="danger">
                You need to select at least one of the <em>Reader</em> or <em>Admin</em> roles.
              </Alert>
            )}
          </>
        </Input>
      </div>
      <div>
        <Headline>Password</Headline>
        <PasswordGroup />
      </div>
    </>
  );
};

export default UserCreate;
