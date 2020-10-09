// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { LdapBackend } from 'logic/authentication/ldap/types';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as GROUP_SYNC_KEY } from '../../BackendWizard/GroupSyncStep';

type Props = {
  authenticationBackend: LdapBackend,
};

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <>
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Groups</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </>
);

const GroupSyncSection = ({ authenticationBackend }: Props) => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');

  const Section = ({ children }: { children: React.Node }) => (
    <SectionComponent title="Group Synchronisation" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={GROUP_SYNC_KEY} />}>
      {children}
    </SectionComponent>
  );

  if (!authGroupSyncPlugins || authGroupSyncPlugins.length <= 0) {
    return (
      <Section>
        <NoEnterpriseComponent />
      </Section>
    );
  }

  const { GroupSyncDetails } = authGroupSyncPlugins[0];

  return (
    <Section>
      <GroupSyncDetails authenticationBackend={authenticationBackend} />
    </Section>
  );
};

export default GroupSyncSection;
