/* eslint-disable camelcase */
// @flow strict
import type { LdapConfigJson, LdapConfig } from './types';

const toJson = ({
  defaultRoles,
  displayNameAttribute,
  serverUri,
  systemUsername,
  trustAllCertificates,
  userSearchBase,
  userSearchPattern,
  useStartTls,
  useSsl,
  type,
}: LdapConfig): LdapConfigJson => ({
  type,
  default_roles: defaultRoles,
  display_name_attribute: displayNameAttribute,
  server_uri: serverUri,
  system_username: systemUsername,
  trust_all_certificates: trustAllCertificates,
  user_search_base: userSearchBase,
  user_search_pattern: userSearchPattern,
  use_start_tls: useStartTls,
  use_ssl: useSsl,
});

const fromJson = ({
  type,
  default_roles,
  display_name_attribute,
  server_uri,
  system_username,
  trust_all_certificates,
  user_search_base,
  user_search_pattern,
  use_start_tls,
  use_ssl,
}: LdapConfigJson): LdapConfig => ({
  type,
  defaultRoles: default_roles,
  displayNameAttribute: display_name_attribute,
  serverUri: server_uri,
  systemUsername: system_username,
  trustAllCertificates: trust_all_certificates,
  userSearchBase: user_search_base,
  userSearchPattern: user_search_pattern,
  useStartTls: use_start_tls,
  useSsl: use_ssl,
});

export default { fromJson, toJson };
