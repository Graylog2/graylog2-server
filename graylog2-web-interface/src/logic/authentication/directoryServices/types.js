// @flow strict
import AuthenticationBackend, { type AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';

type TransportSecurity = ('tls' | 'start_tls' | 'none');
type ServerAddress = { host: string, port: number };

export type DirectoryServiceBackendConfig = {
  servers: Array<ServerAddress>,
  systemUserDn: string,
  systemUserPassword: { isSet: boolean },
  transportSecurity: TransportSecurity,
  type: string,
  userFullNameAttribute: string,
  userNameAttribute: string,
  userSearchBase: string,
  userSearchPattern: string,
  userUniqueIdAttribute: string,
  verifyCertificates: boolean,
};

export type DirectoryServiceBackendConfigJson = {
  servers: Array<ServerAddress>,
  system_user_dn: string,
  system_user_password: { is_set: boolean },
  transport_security: TransportSecurity,
  type: string,
  user_full_name_attribute: string,
  user_name_attribute: string,
  user_search_base: string,
  user_search_pattern: string,
  user_unique_id_attribute: string,
  verify_certificates: boolean,
};

export type DirectoryServiceBackend = {
  id: $PropertyType<AuthenticationBackend, 'id'>,
  defaultRoles: $PropertyType<AuthenticationBackend, 'defaultRoles'>,
  title: $PropertyType<AuthenticationBackend, 'title'>,
  description: $PropertyType<AuthenticationBackend, 'description'>,
  config: DirectoryServiceBackendConfig,
};

export type WizardSubmitPayload = {
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  default_roles: $PropertyType<AuthenticationBackendJSON, 'default_roles'>,
  config: {
    ...DirectoryServiceBackendConfigJson,
    system_user_password: ?(string | { keep_value: true } | { delete_value: true } | { set_value: ?string }),
  },
};
