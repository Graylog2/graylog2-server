// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';

import type { LdapBackend } from 'logic/authentication/ldap/types';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import Role from 'logic/roles/Role';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as USER_SYNC_KEY } from '../../BackendWizard/UserSyncStep';

const RolesList = ({ defaultRolesIds, roles }: {defaultRolesIds: Immutable.List<string>, roles: Immutable.List<Role>}) => {
  const defaultRolesNames = defaultRolesIds.map((roleId) => roles.find((role) => role.id === roleId)?.name);

  return defaultRolesNames.join(', ');
};
type Props = {
  authenticationBackend: LdapBackend,
};

const UserSyncSection = ({ authenticationBackend }: Props) => {
  const [{ list: roles }, setPaginatedRoles] = useState({ list: Immutable.List() });
  const {
    userSearchBase,
    userSearchPattern,
    userNameAttribute,
    userFullNameAttribute,
  } = authenticationBackend.config;
  const {
    defaultRoles = Immutable.List(),
  } = authenticationBackend;

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then((newPaginatedRoles) => newPaginatedRoles && setPaginatedRoles(newPaginatedRoles));
  }, []);

  return (
    <SectionComponent title="User Synchronisation" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={USER_SYNC_KEY} />}>
      <ReadOnlyFormGroup label="Search Base DN" value={userSearchBase} />
      <ReadOnlyFormGroup label="Search Pattern" value={userSearchPattern} />
      <ReadOnlyFormGroup label="Name Attribute" value={userNameAttribute} />
      <ReadOnlyFormGroup label="Full Name Attribute" value={userFullNameAttribute} />
      <ReadOnlyFormGroup label="Default Roles" value={<RolesList roles={roles} defaultRolesIds={defaultRoles} />} />
    </SectionComponent>
  );
};

export default UserSyncSection;
