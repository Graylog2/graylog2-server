// @flow strict
import * as React from 'react';

import { EnterprisePluginNotFound } from 'components/common';
import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const SyncedTeamsSection = ({ authenticationBackend }: Props) => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  if (!EnterpriseSyncedTeamsSection) {
    return (
      <SectionComponent title="Synchronized Teams">
        <EnterprisePluginNotFound featureName="teams" />
      </SectionComponent>
    );
  }

  return <EnterpriseSyncedTeamsSection authenticationBackend={authenticationBackend} />;
};

export default SyncedTeamsSection;
