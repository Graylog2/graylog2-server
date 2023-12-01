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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes, { SECURITY_ROUTES_PREFIX, SECURITY_ROUTE_DESCRIPTION } from 'routing/Routes';
import SecurityPageEntry from 'components/security/SecurityPageEntry';

const routes = [
  { path: `${SECURITY_ROUTES_PREFIX}/*`, component: SecurityPageEntry, parentComponent: ({ children }) => children },
];

export const navigation = {
  description: SECURITY_ROUTE_DESCRIPTION,
  children: [
    { path: Routes.SECURITY.overview(), description: 'Overview' },
    { path: Routes.SECURITY.userActivity(), description: 'User Activity' },
    { path: Routes.SECURITY.hostActivity(), description: 'Host Activity' },
    { path: Routes.SECURITY.networkActivity(), description: 'Network Activity' },
    { path: Routes.SECURITY.anomalies(), description: 'Anomalies' },
  ],
};

const pluginExports: PluginExports = {
  routes,
};

export default pluginExports;
