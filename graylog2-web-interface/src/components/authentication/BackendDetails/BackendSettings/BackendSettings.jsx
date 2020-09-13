// @flow strict
import * as React from 'react';

import ServerConfigSection from './ServerConfigSection';
import UserSyncSection from './UserSyncSection';
import GroupSyncSection from './GroupSyncSection';

import type { LdapService } from '../../ldap/types';

type Props = {
  authenticationBackend: LdapService,
};

const SettingsSection = ({ authenticationBackend }: Props) => (
  <>
    <ServerConfigSection authenticationBackend={authenticationBackend} />
    <UserSyncSection authenticationBackend={authenticationBackend} />
    <GroupSyncSection authenticationBackend={authenticationBackend} />
  </>
);

export default SettingsSection;
