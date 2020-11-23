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

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { DirectoryServiceBackendConfigJson } from 'logic/authentication/directoryServices/types';

export const ldapBackend = AuthenticationBackend.builder()
  .id('ldap-auth-backend-id')
  .title('Ldap authentication backend')
  .description('Ldap authentication backend')
  .defaultRoles(Immutable.List(['reader-role-id']))
  .config({
    servers: [{ host: 'localhost', port: 389 }],
    system_user_dn: '',
    system_user_password: { is_set: false },
    transport_security: 'none',
    type: 'ldap',
    user_full_name_attribute: 'cn',
    user_name_attribute: 'uid',
    user_search_base: 'cn=users,dc=example,dc=com',
    user_search_pattern: '(&(objectClass=person)(sn={0}))',
    user_unique_id_attribute: 'entryUUID',
    verify_certificates: true,
  })
  .build();

export const activeDirectoryBackend = AuthenticationBackend.builder()
  .id('ldap-auth-backend-id')
  .title('Active directory authentication backend')
  .description('Active directory authentication backend')
  .defaultRoles(Immutable.List(['reader-role-id']))
  .config({
    servers: [{ host: 'localhost', port: 636 }],
    system_user_dn: '',
    system_user_password: { is_set: false },
    transport_security: 'tls',
    type: 'active-directory',
    user_full_name_attribute: 'displayName',
    user_name_attribute: 'userPrincipalName',
    user_search_base: 'cn=users,dc=example,dc=com',
    user_search_pattern: '(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))',
    verify_certificates: true,
  })
  .build();
