/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
