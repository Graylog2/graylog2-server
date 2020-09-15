// @flow strict
import * as React from 'react';

import type { LdapBackend } from 'logic/authentication/ldap/types';

import ServerConfigSection from './ServerConfigSection';
import UserSyncSection from './UserSyncSection';
import GroupSyncSection from './GroupSyncSection';

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
