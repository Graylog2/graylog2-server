// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';
import { EnterprisePluginNotFound } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as GROUP_SYNC_KEY } from '../../BackendWizard/GroupSyncStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  roles: Immutable.List<Role>,
};

const GroupSyncSection = ({ authenticationBackend, roles }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const GroupSyncDetails = enterpriseGroupSyncPlugin?.components.GroupSyncDetails;

  return (
    <SectionComponent title="Group Synchronization"
                      headerActions={(
                        <EditLinkButton authenticationBackendId={authenticationBackend.id}
                                        stepKey={GROUP_SYNC_KEY} />
                      )}>
      {GroupSyncDetails ? (
        <GroupSyncDetails authenticationBackend={authenticationBackend} roles={roles} />
      ) : (
        <EnterprisePluginNotFound featureName="group synchronization" />
      )}
    </SectionComponent>
  );
};

export default GroupSyncSection;
