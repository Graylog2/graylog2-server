// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import * as Immutable from 'immutable';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as GROUP_SYNC_KEY } from '../../BackendWizard/GroupSyncStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  roles: Immutable.List<Role>,
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

const GroupSyncSection = ({ authenticationBackend, roles }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const GroupSyncDetails = enterpriseGroupSyncPlugin?.components.GroupSyncDetails;

  return (
    <SectionComponent title="Group Synchronisation" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={GROUP_SYNC_KEY} />}>
      {GroupSyncDetails ? <GroupSyncDetails authenticationBackend={authenticationBackend} roles={roles} /> : <NoEnterpriseComponent />}
    </SectionComponent>
  );
};

export default GroupSyncSection;
