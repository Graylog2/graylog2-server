// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!

type Props = {
  params: {
    name: string,
  },
};

const AuthenticationServiceCreatePage = ({ params: { name } }: Props) => {
  const authServices = PluginStore.exports('authenticationServices') || [];
  const authSerivce = authServices.find((service) => service.name === name);

  console.log(PluginStore.get());

  if (!authSerivce) {
    return `No authentication service configrued for "${name}"`;
  }

  const { setupComponent: ServiceSetup } = authSerivce;

  return <ServiceSetup />;
};

export default withRouter(AuthenticationServiceCreatePage);
