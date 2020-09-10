// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import SectionGrid from 'components/common/Section/SectionGrid';
import AuthenticationService from 'logic/authentication/AuthenticationService';

type Props = {
  authenticationService: AuthenticationService,
};

const AuthenticationServiceDetails = ({ authenticationService }: Props) => {
  const authServices = PluginStore.exports('authenticationServices') || [];
  const authSerivce = authServices.find((service) => service.name === authenticationService.config.type);

  if (!authSerivce) {
    return `No authentication service plugin configrued for active type "${authenticationService.config.type}"`;
  }

  const { detailsComponent: ServiceDetails } = authSerivce;
  console.log('authenticationService', authenticationService)
  return (
    <>
      <SectionGrid>
        <div>
          <ServiceDetails authenticationService={authenticationService} />
        </div>
        <div>
          Users & Teams
        </div>
      </SectionGrid>

    </>
  );
};

export default AuthenticationServiceDetails;
