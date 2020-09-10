// @flow strict
import AuthenticationService from 'logic/authentication/AuthenticationService';

export type LdapService = AuthenticationService & {
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
