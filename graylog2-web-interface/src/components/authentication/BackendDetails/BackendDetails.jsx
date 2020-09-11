// @flow strict
import * as React from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';

import SyncedUsersSection from './SyncedUsersSection';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
  const authService = getAuthServicePlugin(authenticationBackend.config.type);

  if (!authService) {
    return `No authentication service plugin configrued for active type "${authenticationBackend.config.type}"`;
  }

  const { detailsComponent: BackendSettings } = authService;

  return (
    <>
      <SectionGrid>
        <div>
          <BackendSettings authenticationBackend={authenticationBackend} />
        </div>
        <div>
          <SyncedUsersSection authenticationBackend={authenticationBackend} />
        </div>
      </SectionGrid>

    </>
  );
};

export default BackendDetails;
