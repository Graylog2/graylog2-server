// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { EnterprisePluginNotFound } from 'components/common';
import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
import Role from 'logic/roles/Role';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  roles: Immutable.List<Role>,
};

const SyncedTeamsSection = ({ roles }: Props) => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  if (!EnterpriseSyncedTeamsSection) {
    return (
      <SectionComponent title="Synchronized Teams">
        <EnterprisePluginNotFound featureName="teams" />
      </SectionComponent>
    );
  }

  return <EnterpriseSyncedTeamsSection roles={roles} />;
};

export default SyncedTeamsSection;
