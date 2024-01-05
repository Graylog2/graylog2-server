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

import Routes from 'routing/Routes';
import filterMenuItems, { filterCloudMenuItems } from 'util/conditional/filterMenuItems';
import AppConfig from 'util/AppConfig';

export const SYSTEM_DROPDOWN_TITLE = 'System';

const navigationBindings = {
  navigation: [
    {
      path: Routes.SEARCH,
      description: 'Search',
    },
    {
      path: Routes.STREAMS,
      description: 'Streams',
    },
    {
      path: Routes.ALERTS.LIST,
      description: 'Alerts',
    },
    {
      path: Routes.DASHBOARDS,
      description: 'Dashboards',
    },
    {
      description: SYSTEM_DROPDOWN_TITLE,
      position: 'last' as const,
      children: filterCloudMenuItems(
        filterMenuItems(
          [
            { path: Routes.SYSTEM.OVERVIEW, description: 'Overview' },
            { path: Routes.SYSTEM.CONFIGURATIONS, description: 'Configurations', permissions: ['clusterconfigentry:read'] },
            { path: Routes.SYSTEM.NODES.LIST, description: 'Nodes' },
            { path: Routes.SYSTEM.DATANODES.LIST, description: 'Data Nodes' },
            { path: Routes.SYSTEM.INPUTS, description: 'Inputs', permissions: ['inputs:read'] },
            { path: Routes.SYSTEM.OUTPUTS, description: 'Outputs', permissions: ['outputs:read'] },
            { path: Routes.SYSTEM.INDICES.LIST, description: 'Indices', permissions: ['indices:read'] },
            { path: Routes.SYSTEM.LOGGING, description: 'Logging', permissions: ['loggers:read'] },
            { path: Routes.SYSTEM.USERS.OVERVIEW, description: 'Users and Teams', permissions: ['users:list'] },
            { path: Routes.SYSTEM.AUTHZROLES.OVERVIEW, description: 'Roles', permissions: ['roles:read'] },
            { path: Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE, description: 'Authentication', permissions: ['authentication:edit'] },
            { path: Routes.SYSTEM.CONTENTPACKS.LIST, description: 'Content Packs', permissions: ['contentpack:read'] },
            { path: Routes.SYSTEM.GROKPATTERNS, description: 'Grok Patterns', permissions: ['grok_pattern:read'] },
            { path: Routes.SYSTEM.LOOKUPTABLES.OVERVIEW, description: 'Lookup Tables', permissions: ['lookuptables:read'] },
            { path: Routes.SYSTEM.PIPELINES.OVERVIEW, description: 'Pipelines', permissions: ['pipeline:read', 'pipeline_connection:read'] },
            { path: Routes.SYSTEM.SIDECARS.OVERVIEW, description: 'Sidecars', permissions: ['sidecars:read'] },
          ],
          AppConfig.isCloud() && !AppConfig.isFeatureEnabled('cloud_inputs') ? [Routes.SYSTEM.INPUTS] : [],
        ),
        [Routes.SYSTEM.NODES.LIST, Routes.SYSTEM.DATANODES.LIST, Routes.SYSTEM.OUTPUTS, Routes.SYSTEM.LOGGING, Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE],
      ),
    },
  ],
};

export default navigationBindings;
