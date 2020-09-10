// @flow strict
import * as Immutable from 'immutable';

import AuthenticationService from 'logic/authentication/AuthenticationService';

const ldapService = AuthenticationService
  .builder()
  .id('ldap-service-id')
  .title('LDAP Service')
  .description('LDAP service description')
  .config({
    type: 'ldap',
    serverUri: 'ldap://localhost:389',
    systemUsername: 'uid=admin,out=system',
    encryptedSystemPassword: 'encrypted-password',
    userSearchPattern: '(&(|(objectClass=inetOrgPerson))(uid={0}))',
  })
  .build();

const activeDirectoryService = AuthenticationService
  .builder()
  .id('ad-service-id')
  .title('Active Directory Service')
  .description('Active directory service description')
  .config({
    type: 'active-directory',
    serverUri: 'ldap://localhost:389',
    systemUsername: 'uid=admin,out=system',
    encryptedSystemPassword: 'encrypted-password',
    userSearchPattern: '(&(|(objectClass=inetOrgPerson))(uid={0}))',
  })
  .build();

const services = Immutable.List<AuthenticationService>([ldapService, activeDirectoryService]);

export { ldapService, activeDirectoryService, services };
