// @flow strict
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

export type LdapService = {
  id: $PropertyType<AuthenticationBackend, 'id'>,
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: {
    type: 'ldap',
    defaultRoles: Array<string>,
    displayNameAttribute: string,
    encryptedSystemPassword: string,
    serverUri: string,
    systemUsername: string,
    trustAllCertificates: boolean,
    userSearchBase: string,
    userSearchPattern: string,
    useStartTls: boolean,
    useSsl: boolean,
  },
};
