// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionGrid from 'components/common/Section/SectionGrid';

import SyncedUsersSection from './SyncedUsersSection';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
  const authServices = PluginStore.exports('authenticationServices') || [];
  const authSerivce = authServices.find((service) => service.name === authenticationBackend.config.type);

  if (!authSerivce) {
    return `No authentication service plugin configrued for active type "${authenticationBackend.config.type}"`;
  }

  const { detailsComponent: BackendSettings } = authSerivce;

  return (
    <>
      <SectionGrid>
        <div>
          <BackendSettings authenticationBackend={authenticationBackend} />
        </div>
        <div>
          <SyncedUsersSection authenticationBackend={authenticationBackend} />
        </div>
      </SectionGrid>

    </>
  );
};

export default BackendDetails;
