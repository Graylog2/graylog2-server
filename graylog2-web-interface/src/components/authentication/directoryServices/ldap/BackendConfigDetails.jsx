// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';

import ServerConfigSection from '../BackendConfigDetails/ServerConfigSection';
import UserSyncSection from '../BackendConfigDetails/UserSyncSection';
import GroupSyncSection from '../BackendConfigDetails/GroupSyncSection';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  roles: Immutable.List<Role>,
};

const BackendConfigDetails = ({ authenticationBackend, roles }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    excludedFields: groupSyncExcludedFields = {},
  } = enterpriseGroupSyncPlugin?.wizardConfig?.ldap ?? {};
  const excludedFields = { ...groupSyncExcludedFields };

  return (
    <>
      <ServerConfigSection authenticationBackend={authenticationBackend} />
      <UserSyncSection authenticationBackend={authenticationBackend} roles={roles} excludedFields={excludedFields} />
      <GroupSyncSection authenticationBackend={authenticationBackend} roles={roles} excludedFields={excludedFields} />
    </>
  );
};

export default BackendConfigDetails;
