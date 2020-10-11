// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';

import type { LdapBackend } from 'logic/authentication/ldap/types';
import SectionComponent from 'components/common/Section/SectionComponent';

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <>
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Groups</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </>
);

type Props = {
  authenticationBackend: LdapBackend,
};

const SyncedTeamsSection = ({ authenticationBackend }: Props) => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');
  const EnterpriseSyncedTeamsSection = authGroupSyncPlugins?.[0]?.components?.SyncedTeamsSection;

  return (EnterpriseSyncedTeamsSection ? <EnterpriseSyncedTeamsSection authenticationBackend={authenticationBackend} /> : (
    <SectionComponent title="Synchronized Users">
      <NoEnterpriseComponent />
    </SectionComponent>
  )
  );
};

export default SyncedTeamsSection;
