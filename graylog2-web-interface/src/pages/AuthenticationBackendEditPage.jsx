// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Spinner } from 'components/common';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!
import AuthenticationActions from 'actions/authentication/AuthenticationActions';

type Props = {
  params: {
    id: string,
  },
};

const AuthenticationBackendEditPage = ({ params: { id } }: Props) => {
  const [authBackend, setAuthBackend] = useState();

  useEffect(() => {
    AuthenticationActions.load(id).then((newAuthBackend) => newAuthBackend && setAuthBackend(newAuthBackend));
  }, []);

  if (!authBackend) {
    return <Spinner />;
  }

  const authServices = PluginStore.exports('authenticationServices') || [];
  const authSerivce = authServices.find((service) => service.name === authBackend.config.type);

  if (!authSerivce) {
    return `No authentication service plugin configrued for "${authBackend.config.type}"`;
  }

  const { editComponent: ServiceEdit } = authSerivce;

  return <ServiceEdit />;
};

export default withRouter(AuthenticationBackendEditPage);
