// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { EnterprisePluginNotFound } from 'components/common';
import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
import Role from 'logic/roles/Role';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  authenticationBackend: AuthenticationBackend,
  roles: Immutable.List<Role>,
};

const SyncedTeamsSection = ({ roles, authenticationBackend }: Props) => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  if (!EnterpriseSyncedTeamsSection) {
    return (
      <SectionComponent title="Synchronized Teams">
        <EnterprisePluginNotFound featureName="teams" />
      </SectionComponent>
    );
  }

  return <EnterpriseSyncedTeamsSection roles={roles} authenticationBackend={authenticationBackend} />;
};

export default SyncedTeamsSection;
