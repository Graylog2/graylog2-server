// @flow strict
import * as Immutable from 'immutable';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

const ldapService = AuthenticationBackend
  .builder()
  .id('ldap-service-id')
  .title('LDAP: ldap://localhost:389')
  .description('LDAP service description')
  .defaultRoles(Immutable.List(['Reader']))
  .config({
    serverUrls: ['ldap://localhost:389'],
    systemUserDn: 'uid=admin,out=system',
    transportSecurity: 'tls',
    type: 'ldap',
    userFullNameAttribute: 'uid',
    userNameAttribute: 'cn',
    userSearchBase: 'dc=example,dc=com',
    userSearchPattern: '(&(|(objectClass=inetOrgPerson))(uid={0}))',
    verifyCertificates: true,
  })
  .build();

const activeDirectoryService = AuthenticationBackend
  .builder()
  .id('ad-service-id')
  .title('Active Directory: ldap://localhost:389')
  .description('Active directory service description')
  .defaultRoles(Immutable.List(['Reader']))
  .config({
    serverUrls: ['ldap://localhost:389'],
    systemUserDn: 'uid=admin,out=system',
    transportSecurity: 'tls',
    type: 'active-directory',
    userFullNameAttribute: 'uid',
    userNameAttribute: 'cn',
    userSearchBase: 'dc=example,dc=com',
    userSearchPattern: '(&(|(objectClass=inetOrgPerson))(uid={0}))',
    verifyCertificates: true,
  })
  .build();

const services = Immutable.List<AuthenticationBackend>([ldapService, activeDirectoryService]);

export { ldapService, activeDirectoryService, services };
