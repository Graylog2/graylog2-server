/* eslint-disable camelcase */
// @flow strict
import type { LdapConfigJson, LdapConfig } from './types';

const toJson = ({
  serverUrls,
  systemUserDn,
  transportSecurity,
  type,
  userFullNameAttribute,
  userNameAribute,
  userSearchBase,
  userSearchPattern,
  verifyCertificates,
}: LdapConfig): LdapConfigJson => ({
  server_urls: serverUrls,
  system_user_dn: systemUserDn,
  transport_security: transportSecurity,
  type: type,
  user_full_name_attribute: userFullNameAttribute,
  user_name_attribute: userNameAribute,
  user_search_base: userSearchBase,
  user_search_pattern: userSearchPattern,
  verify_certificates: verifyCertificates,
});

const fromJson = ({
  server_urls,
  system_user_dn,
  transport_security,
  type,
  user_full_name_attribute,
  user_name_attribute,
  user_search_base,
  user_search_pattern,
  verify_certificates,
}: LdapConfigJson): LdapConfig => ({
  serverUrls: server_urls,
  systemUserDn: system_user_dn,
  transportSecurity: transport_security,
  type: type,
  userFullNameAttribute: user_full_name_attribute,
  userNameAribute: user_name_attribute,
  userSearchBase: user_search_base,
  userSearchPattern: user_search_pattern,
  verifyCertificates: verify_certificates,
});

export default { fromJson, toJson };
