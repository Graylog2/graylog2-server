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

export const ldapBackend = AuthenticationBackend.builder()
  .id('ldap-auth-backend-id')
  .title('Ldap authentication backend')
  .description('Ldap authentication backend')
  .defaultRoles(Immutable.List(['reader-role-id']))
  .config({
    servers: [{ host: 'localhost', port: 389 }],
    systemUserDn: '',
    systemUserPassword: { isSet: false },
    transportSecurity: 'none',
    type: 'ldap',
    userFullNameAttribute: 'cn',
    userNameAttribute: 'uid',
    userSearchBase: 'cn=users,dc=example,dc=com',
    userSearchPattern: '(&(objectClass=person)(sn={0}))',
    userUniqueIdAttribute: 'entryUUID',
    verifyCertificates: true,
  })
  .build();

export const activeDirectoryBackend = AuthenticationBackend.builder()
  .id('ldap-auth-backend-id')
  .title('Active directory authentication backend')
  .description('Active directory authentication backend')
  .defaultRoles(Immutable.List(['reader-role-id']))
  .config({
    servers: [{ host: 'localhost', port: 636 }],
    systemUserDn: '',
    systemUserPassword: { isSet: false },
    transportSecurity: 'tls',
    type: 'active-directory',
    userFullNameAttribute: 'displayName',
    userNameAttribute: 'userPrincipalName',
    userSearchBase: 'cn=users,dc=example,dc=com',
    userSearchPattern: '(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))',
    userUniqueIdAttribute: undefined,
    verifyCertificates: true,
  })
  .build();
