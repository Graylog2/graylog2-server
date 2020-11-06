// @flow strict
import * as React from 'react';

import { EnterprisePluginNotFound } from 'components/common';
import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
import SectionComponent from 'components/common/Section/SectionComponent';

const SyncedTeamsSection = () => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  if (!EnterpriseSyncedTeamsSection) {
    return (
      <SectionComponent title="Synchronized Teams">
        <EnterprisePluginNotFound featureName="teams" />
      </SectionComponent>
    );
  }

  return <EnterpriseSyncedTeamsSection />;
};

export default SyncedTeamsSection;
