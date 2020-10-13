// @flow strict
import * as React from 'react';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';

import ServerConfigSection from './ServerConfigSection';
import UserSyncSection from './UserSyncSection';
import GroupSyncSection from './GroupSyncSection';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  roles: Immutable.List<Role>,
};

const BackendConfigDetails = ({ authenticationBackend, roles }: Props) => {
  return (
    <>
      <ServerConfigSection authenticationBackend={authenticationBackend} />
      <UserSyncSection authenticationBackend={authenticationBackend} roles={roles} />
      <GroupSyncSection authenticationBackend={authenticationBackend} roles={roles} />
    </>
  );
};

export default BackendConfigDetails;
