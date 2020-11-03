// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Spinner } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';

import SyncedUsersSection from './SyncedUsersSection';
import SyncedTeamsSection from './SyncedTeamsSection';

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetailsActive = ({ authenticationBackend }: Props) => {
  const authService = getAuthServicePlugin(authenticationBackend.config.type);
  const [paginatedRoles, setPaginatedRoles] = useState<?PaginatedRoles>();

  useEffect(() => _loadRoles(setPaginatedRoles), []);

  if (!authService) {
    return `No authentication service plugin configured for type "${authenticationBackend.config.type}"`;
  }

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const { configDetailsComponent: BackendConfigDetails } = authService;

  return (
    <SectionGrid>
      <div>
        <BackendConfigDetails authenticationBackend={authenticationBackend} roles={paginatedRoles.list} />
      </div>
      <div>
        <SyncedUsersSection roles={paginatedRoles.list} />
        <SyncedTeamsSection roles={paginatedRoles.list} />
      </div>
    </SectionGrid>
  );
};

export default BackendDetailsActive;
