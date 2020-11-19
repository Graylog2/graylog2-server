/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/* eslint-disable camelcase */
// @flow strict
import type { DirectoryServiceBackendConfigJson, DirectoryServiceBackendConfig } from './types';

const toJson = ({
  servers,
  systemUserDn,
  systemUserPassword,
  transportSecurity,
  type,
  userFullNameAttribute,
  userNameAttribute,
  userSearchBase,
  userSearchPattern,
  userUniqueIdAttribute,
  verifyCertificates,
}: DirectoryServiceBackendConfig): DirectoryServiceBackendConfigJson => ({
  servers,
  system_user_dn: systemUserDn,
  system_user_password: { is_set: systemUserPassword.isSet },
  transport_security: transportSecurity,
  type: type,
  user_full_name_attribute: userFullNameAttribute,
  user_name_attribute: userNameAttribute,
  user_search_base: userSearchBase,
  user_search_pattern: userSearchPattern,
  user_unique_id_attribute: userUniqueIdAttribute,
  verify_certificates: verifyCertificates,
});

const fromJson = ({
  servers,
  system_user_dn,
  system_user_password,
  transport_security,
  type,
  user_full_name_attribute,
  user_name_attribute,
  user_search_base,
  user_search_pattern,
  user_unique_id_attribute,
  verify_certificates,
}: DirectoryServiceBackendConfigJson): DirectoryServiceBackendConfig => ({
  servers,
  systemUserDn: system_user_dn,
  systemUserPassword: { isSet: system_user_password.is_set },
  transportSecurity: transport_security,
  type: type,
  userFullNameAttribute: user_full_name_attribute,
  userNameAttribute: user_name_attribute,
  userSearchBase: user_search_base,
  userSearchPattern: user_search_pattern,
  userUniqueIdAttribute: user_unique_id_attribute,
  verifyCertificates: verify_certificates,
});

export default { fromJson, toJson };
