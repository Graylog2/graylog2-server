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

import InputsAggregatedDotBadge from 'components/inputs/InputsAggregatedDotBadge';
import Routes from 'routing/Routes';
import filterMenuItems, { filterCloudMenuItems } from 'util/conditional/filterMenuItems';
import AppConfig from 'util/AppConfig';
import DocsHelper from 'util/DocsHelper';

export const SYSTEM_DROPDOWN_TITLE = 'System';
export const SEARCH_LINK_TITLE = 'Search';
export const DASHBOARDS_LINK_TITLE = 'Dashboards';
export const AUTHENTICATION_LINK_TITLE = 'Authentication';

const navigationBindings: PluginExports = {
  navigation: [
    {
      path: Routes.SEARCH,
      description: SEARCH_LINK_TITLE,
      titleKey: 'navigation:main.search',
    },
    {
      path: Routes.STREAMS,
      description: 'Streams',
      titleKey: 'navigation:main.streams',
    },
    {
      path: Routes.ALERTS.LIST,
      description: 'Alerts',
      titleKey: 'navigation:main.alerts',
    },
    {
      path: Routes.DASHBOARDS,
      description: DASHBOARDS_LINK_TITLE,
      titleKey: 'navigation:main.dashboards',
    },
    {
      description: SYSTEM_DROPDOWN_TITLE,
      titleKey: 'navigation:main.system',
      BadgeComponent: InputsAggregatedDotBadge,
      position: { last: true },
      children: filterCloudMenuItems(
        filterMenuItems(
          [
            { path: Routes.SYSTEM.OVERVIEW, description: 'Overview', titleKey: 'navigation:system.overview' },
            {
              path: Routes.SYSTEM.CONFIGURATIONS,
              description: 'Configurations',
              titleKey: 'navigation:system.configurations',
              permissions: ['clusterconfigentry:read'],
            },
            {
              path: Routes.SYSTEM.CLUSTER.NODES,
              description: 'Cluster Configuration',
              titleKey: 'navigation:system.cluster_configuration',
              permissions: ['clusterconfiguration:read'],
            },
            {
              path: Routes.SYSTEM.INPUTS,
              description: 'Inputs',
              titleKey: 'navigation:system.inputs',
              permissions: ['inputs:read'],
              BadgeComponent: InputsAggregatedDotBadge,
            },
            {
              path: Routes.SYSTEM.OUTPUTS,
              description: 'Outputs',
              titleKey: 'navigation:system.outputs',
              permissions: ['outputs:read'],
            },
            {
              path: Routes.SYSTEM.INDICES.LIST,
              description: 'Indices',
              titleKey: 'navigation:system.indices',
              permissions: ['indices:read'],
            },
            {
              path: Routes.SYSTEM.LOGGING,
              description: 'Logging',
              titleKey: 'navigation:system.logging',
              permissions: ['loggers:read'],
            },
            {
              path: Routes.SYSTEM.USERS.OVERVIEW,
              description: 'Users and Teams',
              titleKey: 'navigation:system.users_and_teams',
            },
            {
              path: Routes.SYSTEM.AUTHZROLES.OVERVIEW,
              description: 'Roles',
              titleKey: 'navigation:system.roles',
              permissions: ['roles:read'],
            },
            {
              path: Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE,
              description: AUTHENTICATION_LINK_TITLE,
              titleKey: 'navigation:system.authentication',
              permissions: ['authentication:edit'],
            },
            {
              path: Routes.SYSTEM.CONTENTPACKS.LIST,
              description: 'Content Packs',
              titleKey: 'navigation:system.content_packs',
              permissions: ['contentpack:read'],
            },
            {
              path: Routes.SYSTEM.GROKPATTERNS,
              description: 'Grok Patterns',
              titleKey: 'navigation:system.grok_patterns',
              permissions: ['grok_pattern:read'],
            },
            {
              path: Routes.SYSTEM.LOOKUPTABLES.OVERVIEW,
              description: 'Lookup Tables',
              titleKey: 'navigation:system.lookup_tables',
              permissions: ['lookuptables:read'],
            },
            {
              path: Routes.SYSTEM.PIPELINES.OVERVIEW,
              description: 'Pipelines',
              titleKey: 'navigation:system.pipelines',
              permissions: ['pipeline:read', 'pipeline_connection:read'],
            },
            {
              path: Routes.SYSTEM.SIDECARS.OVERVIEW,
              description: 'Sidecars',
              titleKey: 'navigation:system.sidecars',
              permissions: ['sidecars:read'],
            },
            {
              path: Routes.SYSTEM.COLLECTORS.OVERVIEW,
              description: 'Collectors',
              titleKey: 'navigation:system.collectors',
              permissions: ['collector_fleets:read', 'collectors_config:read'],
              requiredFeatureFlag: 'collectors',
            },
          ],
          AppConfig.isCloud() && !AppConfig.isFeatureEnabled('cloud_inputs') ? [Routes.SYSTEM.INPUTS] : [],
        ),
        [
          Routes.SYSTEM.CLUSTER.NODES,
          Routes.SYSTEM.OUTPUTS,
          Routes.SYSTEM.LOGGING,
          Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE,
        ],
      ),
    },
  ],
  helpMenu: [
    {
      description: 'Documentation',
      titleKey: 'navigation:help.documentation',
      externalLink: DocsHelper.versionedDocsHomePage(),
    },
    ...(AppConfig.gl2DevMode()
      ? [
          {
            description: 'Design System',
            titleKey: 'navigation:help.design_system',
            externalLink: 'https://graylog2.github.io/design-system',
          },
        ]
      : []),
    {
      description: 'Keyboard Shortcuts',
      titleKey: 'navigation:help.keyboard_shortcuts',
      action: ({ showHotkeysModal }) => showHotkeysModal(),
    },
    {
      description: 'API browser',
      titleKey: 'navigation:help.api_browser',
      path: Routes.API_BROWSER,
      permissions: 'api_browser:read',
    },
  ],
};

export default navigationBindings;
