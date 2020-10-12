// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
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

const SyncedTeamsSection = () => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  return (
    EnterpriseSyncedTeamsSection ? <EnterpriseSyncedTeamsSection /> : (
      <SectionComponent title="Synchronized Users">
        <NoEnterpriseComponent />
      </SectionComponent>
    )
  );
};

export default SyncedTeamsSection;
