// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

type Props = {
  params: {
    name: string,
  },
};

const AuthenticationBackendCreatePage = ({ params: { name } }: Props) => {
  const authServices = PluginStore.exports('authenticationServices') || [];
  const authSerivce = authServices.find((service) => service.name === name);

  if (!authSerivce) {
    return `No authentication service plugin configrued for "${name}"`;
  }

  const { createComponent: BackendCreate } = authSerivce;

  return <BackendCreate />;
};

export default withRouter(AuthenticationBackendCreatePage);
