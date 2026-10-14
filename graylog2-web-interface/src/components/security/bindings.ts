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

import Routes, { SECURITY_PATH } from 'routing/Routes';
import SecurityPageEntry from 'components/security/SecurityPageEntry';
import DEFAULT_ENTERPRISE_NAV_ITEM from 'components/navigation/DefaultEnterpriseNavItem';

const routes = [{ path: `${SECURITY_PATH}/*`, component: SecurityPageEntry }];

export const DEFAULT_SECURITY_NAV_ITEM = {
  description: 'Security',
  titleKey: 'navigation:main.security',
  position: { after: DEFAULT_ENTERPRISE_NAV_ITEM.description },
  children: [
    { path: Routes.SECURITY.OVERVIEW, description: 'Overview', titleKey: 'navigation:security.overview' },
    {
      path: Routes.SECURITY.USER_ACTIVITY,
      description: 'User Activity',
      titleKey: 'navigation:security.user_activity',
    },
    {
      path: Routes.SECURITY.HOST_ACTIVITY,
      description: 'Host Activity',
      titleKey: 'navigation:security.host_activity',
    },
    {
      path: Routes.SECURITY.NETWORK_ACTIVITY,
      description: 'Network Activity',
      titleKey: 'navigation:security.network_activity',
    },
    { path: Routes.SECURITY.ANOMALIES, description: 'Anomalies', titleKey: 'navigation:security.anomalies' },
  ],
};

const pluginExports: PluginExports = {
  routes,
};

export default pluginExports;
