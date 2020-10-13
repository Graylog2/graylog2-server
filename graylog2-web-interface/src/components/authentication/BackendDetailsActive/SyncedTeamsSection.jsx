// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
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
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseGroupSyncPlugin?.components.SyncedTeamsSection;

  return (
    EnterpriseSyncedTeamsSection ? <EnterpriseSyncedTeamsSection /> : (
      <SectionComponent title="Synchronized Users">
        <NoEnterpriseComponent />
      </SectionComponent>
    )
  );
};

export default SyncedTeamsSection;
