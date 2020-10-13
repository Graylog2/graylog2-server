// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import { Spinner } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import type { LdapBackend } from 'logic/authentication/ldap/types';

import ServerConfigSection from './ServerConfigSection';
import UserSyncSection from './UserSyncSection';
import GroupSyncSection from './GroupSyncSection';

type Props = {
  authenticationBackend: LdapBackend,
};

const BackendConfigDetails = ({ authenticationBackend }: Props) => {
  const [{ list: roles }, setPaginatedRoles] = useState({ list: undefined });

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then((newPaginatedRoles) => newPaginatedRoles && setPaginatedRoles(newPaginatedRoles));
  }, []);

  if (!roles) {
    return <Spinner />;
  }

  return (
    <>
      <ServerConfigSection authenticationBackend={authenticationBackend} />
      <UserSyncSection authenticationBackend={authenticationBackend} roles={roles} />
      <GroupSyncSection authenticationBackend={authenticationBackend} roles={roles} />
    </>
  );
};

export default BackendConfigDetails;
