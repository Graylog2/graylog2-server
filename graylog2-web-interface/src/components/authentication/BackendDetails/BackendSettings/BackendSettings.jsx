// @flow strict
import * as React from 'react';

import type { LdapBackend } from 'logic/authentication/ldap/types';

import GroupSyncSection from './GroupSyncSection';
import ServerConfigSection from './ServerConfigSection';
import UserSyncSection from './UserSyncSection';

type Props = {
  authenticationBackend: LdapBackend,
};

const SettingsSection = ({ authenticationBackend }: Props) => (
  <>
    <ServerConfigSection authenticationBackend={authenticationBackend} />
    <UserSyncSection authenticationBackend={authenticationBackend} />
    <GroupSyncSection authenticationBackend={authenticationBackend} />
  </>
);

export default SettingsSection;
