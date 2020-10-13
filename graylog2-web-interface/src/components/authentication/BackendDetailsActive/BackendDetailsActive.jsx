// @flow strict
import * as React from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';

import SyncedUsersSection from './SyncedUsersSection';
import SyncedTeamsSection from './SyncedTeamsSection';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetailsActive = ({ authenticationBackend }: Props) => {
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
        <SyncedUsersSection />
        <SyncedTeamsSection />
      </div>
    </SectionGrid>
  );
};

export default BackendDetailsActive;
