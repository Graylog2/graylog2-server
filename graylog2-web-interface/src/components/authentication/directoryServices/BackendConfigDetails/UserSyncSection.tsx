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
import * as React from 'react';
import * as Immutable from 'immutable';
import * as PropTypes from 'prop-types';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as USER_SYNC_KEY } from '../BackendWizard/UserSyncStep';

const rolesList = (defaultRolesIds: Immutable.List<string>, roles: Immutable.List<Role>) => {
  const defaultRolesNames = defaultRolesIds.map((roleId) => roles.find((role) => role.id === roleId)?.name ?? 'Role not found');

  return defaultRolesNames.join(', ');
};

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  excludedFields: {[ inputName: string ]: boolean },
  roles: Immutable.List<Role>,
};

const UserSyncSection = ({ authenticationBackend, roles, excludedFields }: Props) => {
  const {
    userSearchBase,
    userSearchPattern,
    userNameAttribute,
    userFullNameAttribute,
    userUniqueIdAttribute,
  } = authenticationBackend.config;
  const {
    defaultRoles = Immutable.List(),
  } = authenticationBackend;

  return (
    <SectionComponent title="User Synchronization" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={USER_SYNC_KEY} />}>
      <ReadOnlyFormGroup label="Search Base DN" value={userSearchBase} />
      <ReadOnlyFormGroup label="Search Pattern" value={userSearchPattern} />
      <ReadOnlyFormGroup label="Name Attribute" value={userNameAttribute} />
      <ReadOnlyFormGroup label="Full Name Attribute" value={userFullNameAttribute} />
      {!excludedFields.userUniqueIdAttribute && (
        <ReadOnlyFormGroup label="ID Attribute" value={userUniqueIdAttribute} />
      )}
      <ReadOnlyFormGroup label="Default Roles" value={rolesList(defaultRoles, roles)} />
    </SectionComponent>
  );
};

UserSyncSection.defaultProps = {
  excludedFields: {},
};

UserSyncSection.propTypes = {
  excludedFields: PropTypes.object,
};

export default UserSyncSection;
