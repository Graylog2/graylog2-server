// @flow strict
import * as React from 'react';

import ServerConfiguration from './ServerConfigSection';
import UserSyncSettings from './UserSyncSection';
import GroupSyncSettings from './GroupSyncSection';

import type { LdapService } from '../../ldap/types';

type Props = {
  authenticationBackend: LdapService,
};

const SettingsSection = ({ authenticationBackend }: Props) => (
  <>
    <ServerConfiguration authenticationBackend={authenticationBackend} />
    <UserSyncSettings authenticationBackend={authenticationBackend} />
    <GroupSyncSettings authenticationBackend={authenticationBackend} />
  </>
);

export default SettingsSection;
