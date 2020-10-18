// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Spinner } from 'components/common';
import { Alert } from 'components/graylog';
import SectionGrid from 'components/common/Section/SectionGrid';
import SectionComponent from 'components/common/Section/SectionComponent';

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
  const [paginatedRoles, setPaginatedRoles] = useState<?PaginatedRoles>();
  const authService = getAuthServicePlugin(authenticationBackend.config.type);

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
        <SectionComponent title="Synchronized Users">
          <Alert>
            Managing synchronized users is only possible for the active authentication service.
          </Alert>
        </SectionComponent>
        <SectionComponent title="Synchronized Teams">
          <Alert>
            Managing synchronized teams is only possible for the active authentication service.
          </Alert>
        </SectionComponent>
      </div>
    </SectionGrid>
  );
};

export default BackendDetails;
