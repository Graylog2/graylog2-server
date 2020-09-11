// @flow strict
import * as React from 'react';

import ServerConfiguration from './ServerConfigSection';
import UserSyncSettings from './UserSyncSection';
import GroupSyncSettings from './GroupSyncSection';

import type { LdapService } from '../types';

type Props = {
  authenticationService: LdapService,
};

const ServiceSettings = ({ authenticationService }: Props) => {
  return (
    <>
      <ServerConfiguration authenticationService={authenticationService} />
      <UserSyncSettings authenticationService={authenticationService} />
      <GroupSyncSettings authenticationService={authenticationService} />
    </>
  );
};

export default ServiceSettings;
