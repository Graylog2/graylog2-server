// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import SectionComponent from 'components/common/Section/SectionComponent';

import type { LdapService } from '../types';

type Props = {
  authenticationService: LdapService,
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

const GroupSyncSection = ({ authenticationService }: Props) => {
  const authenticationPlugin = PluginStore.exports('authenticationServices.ldap');
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE(authenticationService.config.type),
    query: {
      step: 'groupSync',
    },
  };

  const Section = ({ children }: { children: React.Node }) => (
    <SectionComponent title="Group Synchronisation" subTitle={<Link to={editLink}>Edit</Link>}>
      {children}
    </SectionComponent>
  );

  if (!authenticationPlugin || authenticationPlugin.length <= 0) {
    return (
      <Section>
        <NoEnterpriseComponent />
      </Section>
    );
  }

  const { GroupSyncDetails } = authenticationPlugin[0];

  return (
    <Section>
      <GroupSyncDetails authenticationService={authenticationService} />
    </Section>
  );
};

export default GroupSyncSection;
