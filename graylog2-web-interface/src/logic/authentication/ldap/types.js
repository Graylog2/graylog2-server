// @flow strict
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

export type LdapConfig = {
  type: string,
  defaultRoles: Array<string>,
  displayNameAttribute: string,
  serverUri: string,
  systemUsername: string,
  trustAllCertificates: boolean,
  userSearchBase: string,
  userSearchPattern: string,
  useStartTls: boolean,
  useSsl: boolean,
};

export type LdapConfigJson = {
  type: string,
  default_roles: Array<string>,
  display_name_attribute: string,
  server_uri: string,
  system_username: string,
  trust_all_certificates: boolean,
  user_search_base: string,
  user_search_pattern: string,
  use_start_tls: boolean,
  use_ssl: boolean,
};

export type LdapBackend = {
  id: $PropertyType<AuthenticationBackend, 'id'>,
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: LdapConfig,
};

export type LdapCreate = {
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: LdapConfigJson,
};

export type LdapUpdate = LdapCreate & {
  id: $PropertyType<AuthenticationBackend, 'id'>,
};
