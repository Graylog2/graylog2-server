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
