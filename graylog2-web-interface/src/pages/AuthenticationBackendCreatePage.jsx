// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';

import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import { getAuthServicePlugin } from 'util/AuthenticationService';

type Props = {
  params: {
    name: string,
  },
};

const AuthenticationBackendCreatePage = ({ params: { name } }: Props) => {
  const authService = getAuthServicePlugin(name);

  if (!authService) {
    return `No authentication service plugin configured for type "${name}"`;
  }

  const { createComponent: BackendCreate } = authService;

  return <BackendCreate />;
};

export default withRouter(AuthenticationBackendCreatePage);
