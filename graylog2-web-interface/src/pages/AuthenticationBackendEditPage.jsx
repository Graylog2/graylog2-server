// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';

import withParams from 'routing/withParams';
import withLocation, { type Location } from 'routing/withLocation';
import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import { getAuthServicePlugin } from 'util/AuthenticationService';
import { Spinner } from 'components/common';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

type Props = {
  params: {
    backendId: string,
  },
  location: Location,
};

const AuthenticationBackendEditPage = ({ params: { backendId }, location: { query: { initialStepKey } } }: Props) => {
  const [authBackend, setAuthBackend] = useState();

  useEffect(() => {
    AuthenticationDomain.load(backendId).then((response) => setAuthBackend(response.backend));
  }, []);

  if (!authBackend) {
    return <Spinner />;
  }

  const authService = getAuthServicePlugin(authBackend.config.type);

  if (!authService) {
    return `No authentication service plugin configured for "${authBackend.config.type}"`;
  }

  const { editComponent: BackendEdit } = authService;

  return <BackendEdit authenticationBackend={authBackend} initialStepKey={initialStepKey} />;
};

export default withParams(withLocation(AuthenticationBackendEditPage));
