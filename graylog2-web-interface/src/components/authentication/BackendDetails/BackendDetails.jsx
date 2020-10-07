// @flow strict
import * as React from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';
import { Alert } from 'components/graylog';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
  const authService = getAuthServicePlugin(authenticationBackend.config.type);

  if (!authService) {
    return `No authentication service plugin configured for type "${authenticationBackend.config.type}"`;
  }

  const { configDetailsComponent: BackendConfigDetails } = authService;

  return (
    <SectionGrid>
      <div>
        <BackendConfigDetails authenticationBackend={authenticationBackend} />
      </div>
      <div>
        <SectionComponent title="Synchronized Users">
          <Alert>
            Managing synchronized users is only possible for the active authentication service.
          </Alert>
        </SectionComponent>
      </div>
    </SectionGrid>
  );
};

export default BackendDetails;
