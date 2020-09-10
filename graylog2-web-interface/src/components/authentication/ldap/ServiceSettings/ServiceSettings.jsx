// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import SectionComponent from 'components/common/Section/SectionComponent';
import AuthenticationService from 'logic/authentication/AuthenticationService';
import { ReadOnlyFormGroup } from 'components/common';
import type { LdapService } from '../types'
import ServerConfiguration from './ServerConfigSection'
import UserSyncSettings from './UserSyncSection';
import GroupSyncSettings from './GroupSyncSection';

type Props = {
  authenticationService: LdapService
};

const ServiceSettings = ({ authenticationService }: Props) => {
  return (
    <>
      <ServerConfiguration authenticationService={authenticationService} />
      <UserSyncSettings authenticationService={authenticationService} />
      
    </>
  );
};

export default ServiceSettings;
