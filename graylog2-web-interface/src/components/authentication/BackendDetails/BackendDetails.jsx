// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import { Spinner } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { getAuthServicePlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';
import { Alert } from 'components/graylog';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
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
