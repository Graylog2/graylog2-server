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
import { $PropertyType } from 'utility-types';

import AuthenticationBackend, { AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';

type TransportSecurity = ('tls' | 'start_tls' | 'none');
type ServerAddress = { host: string, port: number };

export type DirectoryServiceBackendConfig = {
  servers: Array<ServerAddress>,
  systemUserDn: string,
  systemUserPassword: { isSet: boolean },
  transportSecurity: TransportSecurity,
  type: string,
  userFullNameAttribute: string,
  userNameAttribute: string,
  userSearchBase: string,
  userSearchPattern: string,
  userUniqueIdAttribute: string,
  verifyCertificates: boolean,
};

/* eslint-disable camelcase */
export type DirectoryServiceBackendConfigJson = {
  servers: Array<ServerAddress>,
  system_user_dn: string,
  system_user_password: { is_set: boolean },
  transport_security: TransportSecurity,
  type: string,
  user_full_name_attribute: string,
  user_name_attribute: string,
  user_search_base: string,
  user_search_pattern: string,
  user_unique_id_attribute: string,
  verify_certificates: boolean,
};
/* eslint-enable camelcase */

export type DirectoryServiceBackend = {
  id: $PropertyType<AuthenticationBackend, 'id'>,
  defaultRoles: $PropertyType<AuthenticationBackend, 'defaultRoles'>,
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: DirectoryServiceBackendConfig,
};

/* eslint-disable camelcase */
export type WizardSubmitPayload = {
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  default_roles: $PropertyType<AuthenticationBackendJSON, 'default_roles'>,
  config: DirectoryServiceBackendConfigJson & {
    system_user_password: (string | { keep_value: true } | { delete_value: true } | { set_value: string | undefined }) | undefined,
  },
};
/* eslint-enable camelcase */
