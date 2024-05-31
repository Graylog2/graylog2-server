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
      path: Routes.unqualified.SEARCH,
      description: 'Search',
    },
    {
      path: Routes.unqualified.STREAMS,
      description: 'Streams',
    },
    {
      path: Routes.unqualified.ALERTS.LIST,
      description: 'Alerts',
    },
    {
      path: Routes.unqualified.DASHBOARDS,
      description: 'Dashboards',
    },
    {
      description: SYSTEM_DROPDOWN_TITLE,
      position: 'last' as const,
      children: filterCloudMenuItems(
        filterMenuItems(
          [
            { path: Routes.unqualified.SYSTEM.OVERVIEW, description: 'Overview' },
            { path: Routes.unqualified.SYSTEM.CONFIGURATIONS, description: 'Configurations', permissions: ['clusterconfigentry:read'] },
            { path: Routes.unqualified.SYSTEM.NODES.LIST, description: 'Nodes' },
            { path: Routes.unqualified.SYSTEM.DATANODES.LIST, description: 'Data Nodes' },
            { path: Routes.unqualified.SYSTEM.INPUTS, description: 'Inputs', permissions: ['inputs:read'] },
            { path: Routes.unqualified.SYSTEM.OUTPUTS, description: 'Outputs', permissions: ['outputs:read'] },
            { path: Routes.unqualified.SYSTEM.INDICES.LIST, description: 'Indices', permissions: ['indices:read'] },
            { path: Routes.unqualified.SYSTEM.LOGGING, description: 'Logging', permissions: ['loggers:read'] },
            { path: Routes.unqualified.SYSTEM.USERS.OVERVIEW, description: 'Users and Teams', permissions: ['users:list'] },
            { path: Routes.unqualified.SYSTEM.AUTHZROLES.OVERVIEW, description: 'Roles', permissions: ['roles:read'] },
            { path: Routes.unqualified.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE, description: 'Authentication', permissions: ['authentication:edit'] },
            { path: Routes.unqualified.SYSTEM.CONTENTPACKS.LIST, description: 'Content Packs', permissions: ['contentpack:read'] },
            { path: Routes.unqualified.SYSTEM.GROKPATTERNS, description: 'Grok Patterns', permissions: ['grok_pattern:read'] },
            { path: Routes.unqualified.SYSTEM.LOOKUPTABLES.OVERVIEW, description: 'Lookup Tables', permissions: ['lookuptables:read'] },
            { path: Routes.unqualified.SYSTEM.PIPELINES.OVERVIEW, description: 'Pipelines', permissions: ['pipeline:read', 'pipeline_connection:read'] },
            { path: Routes.unqualified.SYSTEM.SIDECARS.OVERVIEW, description: 'Sidecars', permissions: ['sidecars:read'] },
          ],
          AppConfig.isCloud() && !AppConfig.isFeatureEnabled('cloud_inputs') ? [Routes.unqualified.SYSTEM.INPUTS] : [],
        ),
        [Routes.unqualified.SYSTEM.NODES.LIST, Routes.unqualified.SYSTEM.DATANODES.LIST, Routes.unqualified.SYSTEM.OUTPUTS, Routes.unqualified.SYSTEM.LOGGING, Routes.unqualified.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE],
      ),
    },
  ],
};

export default navigationBindings;
