// @flow strict
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

export type LdapService = AuthenticationBackend & {
  config: {
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
