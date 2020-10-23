// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';
import { Spinner } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';

import SyncedUsersSection from './SyncedUsersSection';
import SyncedTeamsSection from './SyncedTeamsSection';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetailsActive = ({ authenticationBackend }: Props) => {
  const [{ list: roles }, setPaginatedRoles] = useState({ list: undefined });
  const authService = getAuthServicePlugin(authenticationBackend.config.type);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then(setPaginatedRoles);
  }, []);

  if (!authService) {
    return `No authentication service plugin configured for type "${authenticationBackend.config.type}"`;
  }

  if (!roles) {
    return <Spinner />;
  }

  const { configDetailsComponent: BackendConfigDetails } = authService;

  return (
    <SectionGrid>
      <div>
        <BackendConfigDetails authenticationBackend={authenticationBackend} roles={roles} />
      </div>
      <div>
        <SyncedUsersSection roles={roles} />
        <SyncedTeamsSection roles={roles} />
      </div>
    </SectionGrid>
  );
};

export default BackendDetailsActive;
