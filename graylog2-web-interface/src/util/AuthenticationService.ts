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
// @flow strict
import { PluginStore } from 'graylog-web-plugin/plugin';

// eslint-disable-next-line import/prefer-default-export
export const getAuthServicePlugin = (type: string, throwError: boolean = false) => {
  const authServices = PluginStore.exports('authentication.services') || [];
  const authService = authServices.find((service) => service.name === type);

  if (!authService && throwError) {
    throw new Error(`Authentication service with type "${type}" not found.`);
  }

  return authService;
};

export const getEnterpriseGroupSyncPlugin = () => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.directoryServices.groupSync');

  return authGroupSyncPlugins?.[0];
};

export const getEnterpriseAuthenticationPlugin = () => {
  const authPlugins = PluginStore.exports('authentication.enterprise');

  return authPlugins?.[0];
};
