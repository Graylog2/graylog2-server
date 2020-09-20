// @flow strict
import AuthenticationBackend, { type AuthenticationBackendJson } from 'logic/authentication/AuthenticationBackend';

type TransportSecurity = ?('tls' | 'start_tls');

export type LdapConfig = {
  serverUrls: Array<string>,
  systemUserDn: ?string,
  systemUserPassword: { isSet: boolean },
  transportSecurity: TransportSecurity,
  type: string,
  userFullNameAttribute: string,
  userNameAttribute: string,
  userSearchBase: string,
  userSearchPattern: string,
  verifyCertificates: boolean,
};

export type LdapConfigJson = {
  server_urls: Array<string>,
  system_user_dn: ?string,
  system_user_password: { is_set: boolean },
  transport_security: TransportSecurity,
  type: string,
  user_full_name_attribute: string,
  user_name_attribute: string,
  user_search_base: string,
  user_search_pattern: string,
  verify_certificates: boolean,
};

export type LdapBackend = {
  id: $PropertyType<AuthenticationBackend, 'id'>,
  defaultRoles: $PropertyType<AuthenticationBackend, 'defaultRoles'>,
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: LdapConfig,
};

export type LdapCreate = {
  title: $PropertyType<AuthenticationBackendJson, 'title'>,
  description: $PropertyType<AuthenticationBackendJson, 'description'>,
  default_roles: $PropertyType<AuthenticationBackendJson, 'default_roles'>,
  config: {
    ...LdapConfigJson,
    system_user_password: ?string,
  },
};

export type LdapUpdate = {
  ...LdapCreate,
  id: $PropertyType<AuthenticationBackendJson, 'id'>,
};
