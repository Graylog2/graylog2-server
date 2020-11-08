// @flow strict
import * as Immutable from 'immutable';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';

import type { WizardFormValues } from './BackendWizard/BackendWizardContext';

export default ({
  title,
  description,
  defaultRoles = Immutable.List(),
  config: {
    servers = [],
    systemUserDn,
    transportSecurity,
    userFullNameAttribute,
    userNameAttribute,
    userSearchBase,
    userSearchPattern,
    userUniqueIdAttribute,
    verifyCertificates,
  },
}: DirectoryServiceBackend): WizardFormValues => ({
  title,
  description,
  defaultRoles: defaultRoles.join(),
  serverHost: servers[0].host,
  serverPort: servers[0].port,
  systemUserDn,
  transportSecurity,
  userFullNameAttribute,
  userNameAttribute,
  userSearchBase,
  userSearchPattern,
  userUniqueIdAttribute,
  verifyCertificates,
});
