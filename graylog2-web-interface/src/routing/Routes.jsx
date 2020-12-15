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
import { PluginStore } from 'graylog-web-plugin/plugin';
import URI from 'urijs';

import AppConfig from 'util/AppConfig';
import { extendedSearchPath, viewsPath } from 'views/Constants';

const Routes = {
  STARTPAGE: '/',
  NOTFOUND: '/notfound',
  SEARCH: '/search',
  STREAMS: '/streams',
  LEGACY_ALERTS: {
    LIST: '/legacy/alerts',
    CONDITIONS: '/legacy/alerts/conditions',
    NEW_CONDITION: '/legacy/alerts/conditions/new',
    NOTIFICATIONS: '/legacy/alerts/notifications',
    NEW_NOTIFICATION: '/legacy/alerts/notifications/new',
  },
  ALERTS: {
    LIST: '/alerts',
    DEFINITIONS: {
      LIST: '/alerts/definitions',
      CREATE: '/alerts/definitions/new',
      edit: (definitionId) => `/alerts/definitions/${definitionId}/edit`,
      show: (definitionId) => `/alerts/definitions/${definitionId}`,
    },
    NOTIFICATIONS: {
      LIST: '/alerts/notifications',
      CREATE: '/alerts/notifications/new',
      edit: (notificationId) => `/alerts/notifications/${notificationId}/edit`,
      show: (notificationId) => `/alerts/notifications/${notificationId}`,
    },
  },
  SOURCES: '/sources',
  DASHBOARDS: '/dashboards',
  GETTING_STARTED: '/gettingstarted',
  SYSTEM: {
    CONFIGURATIONS: '/system/configurations',
    CONTENTPACKS: {
      LIST: '/system/contentpacks',
      EXPORT: '/system/contentpacks/export',
      CREATE: '/system/contentpacks/create',
      edit: (contentPackId, contentPackRev) => { return `/system/contentpacks/${contentPackId}/${contentPackRev}/edit`; },
      show: (contentPackId) => `/system/contentpacks/${contentPackId}`,
    },
    GROKPATTERNS: '/system/grokpatterns',
    INDICES: {
      LIST: '/system/indices',
      FAILURES: '/system/indices/failures',
    },
    INDEX_SETS: {
      CONFIGURATION: (indexSetId, from) => {
        if (from) {
          return `/system/index_sets/${indexSetId}/configuration?from=${from}`;
        }

        return `/system/index_sets/${indexSetId}/configuration`;
      },
      SHOW: (indexSetId) => `/system/index_sets/${indexSetId}`,
      CREATE: '/system/index_sets/create',
    },
    INPUTS: '/system/inputs',
    LOGGING: '/system/logging',
    METRICS: (nodeId) => `/system/metrics/node/${nodeId}`,
    NODES: {
      LIST: '/system/nodes',
      SHOW: (nodeId) => `/system/nodes/${nodeId}`,
    },
    THREADDUMP: (nodeId) => `/system/threaddump/${nodeId}`,
    OUTPUTS: '/system/outputs',
    OVERVIEW: '/system/overview',
    PROCESSBUFFERDUMP: (nodeId) => `/system/processbufferdump/${nodeId}`,
    AUTHENTICATION: {
      BACKENDS: {
        OVERVIEW: '/system/authentication/services',
        ACTIVE: '/system/authentication/services/active',
        CREATE: '/system/authentication/services/create',
        createBackend: (name) => `/system/authentication/services/create/${name}`,
        show: (id) => `/system/authentication/services/${id}`,
        edit: (id, initialStepKey) => {
          const editUrl = `/system/authentication/services/edit/${id}`;

          if (initialStepKey) return `${editUrl}?initialStepKey=${initialStepKey}`;

          return editUrl;
        },
      },
      AUTHENTICATORS: {
        SHOW: '/system/authentication/authenticator',
        EDIT: '/system/authentication/authenticator/edit',
      },
    },
    USERS: {
      CREATE: '/system/users/new',
      edit: (userId) => `/system/users/edit/${userId}`,
      TOKENS: {
        edit: (userId) => `/system/users/tokens/${userId}`,
      },
      OVERVIEW: '/system/users',
      show: (userId) => `/system/users/${userId}`,
    },
    AUTHZROLES: {
      OVERVIEW: '/system/roles',
      show: (roleId) => `/system/roles/${roleId}`,
      edit: (roleId) => `/system/roles/edit/${roleId}`,
    },
    LOOKUPTABLES: {
      OVERVIEW: '/system/lookuptables',
      CREATE: '/system/lookuptables/create',
      show: (tableName) => `/system/lookuptables/table/${tableName}`,
      edit: (tableName) => `/system/lookuptables/table/${tableName}/edit`,
      CACHES: {
        OVERVIEW: '/system/lookuptables/caches',
        CREATE: '/system/lookuptables/caches/create',
        show: (cacheName) => `/system/lookuptables/caches/${cacheName}`,
        edit: (cacheName) => `/system/lookuptables/caches/${cacheName}/edit`,
      },
      DATA_ADAPTERS: {
        OVERVIEW: '/system/lookuptables/data_adapters',
        CREATE: '/system/lookuptables/data_adapters/create',
        show: (adapterName) => `/system/lookuptables/data_adapter/${adapterName}`,
        edit: (adapterName) => `/system/lookuptables/data_adapter/${adapterName}/edit`,
      },
    },
    PIPELINES: {
      OVERVIEW: '/system/pipelines',
      PIPELINE: (pipelineId) => `/system/pipelines/${pipelineId}`,
      RULES: '/system/pipelines/rules',
      RULE: (ruleId) => `/system/pipelines/rules/${ruleId}`,
      SIMULATOR: '/system/pipelines/simulate',
    },
    ENTERPRISE: '/system/enterprise',
    SIDECARS: {
      OVERVIEW: '/system/sidecars',
      STATUS: (sidecarId) => `/system/sidecars/${sidecarId}/status`,
      ADMINISTRATION: '/system/sidecars/administration',
      CONFIGURATION: '/system/sidecars/configuration',
      NEW_CONFIGURATION: '/system/sidecars/configuration/new',
      EDIT_CONFIGURATION: (configurationId) => `/system/sidecars/configuration/edit/${configurationId}`,
      NEW_COLLECTOR: '/system/sidecars/collector/new',
      EDIT_COLLECTOR: (collectorId) => `/system/sidecars/collector/edit/${collectorId}`,
    },
  },
  VIEWS: {
    LIST: viewsPath,
    VIEWID: (id) => `${viewsPath}/${id}`,
  },
  EXTENDEDSEARCH: extendedSearchPath,
  search_with_query: (query, rangeType, timeRange) => {
    const route = new URI(Routes.SEARCH);
    const queryParams = {
      q: query,
    };

    if (rangeType && timeRange) {
      queryParams[rangeType] = timeRange;
    }

    route.query(queryParams);

    return route.resource();
  },
  _common_search_url: (resource, query, timeRange, resolution) => {
    const route = new URI(resource);
    const queryParams = {
      q: query,
      interval: resolution,
    };

    if (timeRange) {
      Object.keys(timeRange).forEach((key) => {
        queryParams[key] = timeRange[key];
      });
    }

    route.query(queryParams);

    return route.resource();
  },
  search: (query, timeRange, resolution) => {
    return Routes._common_search_url(Routes.SEARCH, query, timeRange, resolution);
  },
  message_show: (index, messageId) => `/messages/${index}/${messageId}`,
  stream_edit: (streamId) => `/streams/${streamId}/edit`,
  stream_edit_example: (streamId, index, messageId) => `${Routes.stream_edit(streamId)}?index=${index}&message_id=${messageId}`,
  stream_outputs: (streamId) => `/streams/${streamId}/outputs`,
  stream_search: (streamId, query, timeRange, resolution) => {
    return Routes._common_search_url(`${Routes.STREAMS}/${streamId}/search`, query, timeRange, resolution);
  },
  stream_alerts: (streamId) => `/alerts/?stream_id=${streamId}`,

  legacy_stream_search: (streamId) => `/streams/${streamId}/messages`,
  show_alert: (alertId) => `${Routes.LEGACY_ALERTS.LIST}/${alertId}`,
  show_alert_condition: (streamId, conditionId) => `${Routes.LEGACY_ALERTS.CONDITIONS}/${streamId}/${conditionId}`,
  new_alert_condition_for_stream: (streamId) => `${Routes.LEGACY_ALERTS.NEW_CONDITION}?stream_id=${streamId}`,
  new_alert_notification_for_stream: (streamId) => `${Routes.LEGACY_ALERTS.NEW_NOTIFICATION}?stream_id=${streamId}`,

  dashboard_show: (dashboardId) => `/dashboards/${dashboardId}`,

  node: (nodeId) => `/system/nodes/${nodeId}`,

  node_inputs: (nodeId) => `${Routes.SYSTEM.INPUTS}/${nodeId}`,
  global_input_extractors: (inputId) => `/system/inputs/${inputId}/extractors`,
  local_input_extractors: (nodeId, inputId) => `/system/inputs/${nodeId}/${inputId}/extractors`,
  export_extractors: (nodeId, inputId) => `${Routes.local_input_extractors(nodeId, inputId)}/export`,
  import_extractors: (nodeId, inputId) => `${Routes.local_input_extractors(nodeId, inputId)}/import`,
  new_extractor: (nodeId, inputId, extractorType, fieldName, index, messageId) => {
    const route = new URI(`/system/inputs/${nodeId}/${inputId}/extractors/new`);
    const queryParams = {
      extractor_type: extractorType,
      field: fieldName,
      example_index: index,
      example_id: messageId,
    };

    route.search(queryParams);

    return route.resource();
  },
  edit_extractor: (nodeId, inputId, extractorId) => `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,

  edit_input_extractor: (nodeId, inputId, extractorId) => `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,
  getting_started: (fromMenu) => `${Routes.GETTING_STARTED}?menu=${fromMenu}`,
  filtered_metrics: (nodeId, filter) => `${Routes.SYSTEM.METRICS(nodeId)}?filter=${filter}`,
};

const qualifyUrls = (routes, appPrefix) => {
  const qualifiedRoutes = {};

  Object.keys(routes).forEach((routeName) => {
    switch (typeof routes[routeName]) {
      case 'string':
        qualifiedRoutes[routeName] = new URI(`${appPrefix}/${routes[routeName]}`).normalizePath().resource();
        break;
      case 'function':
        qualifiedRoutes[routeName] = (...params) => {
          const result = routes[routeName](...params);

          return new URI(`${appPrefix}/${result}`).normalizePath().resource();
        };

        break;
      case 'object':
        qualifiedRoutes[routeName] = qualifyUrls(routes[routeName], appPrefix);
        break;
      default:
        break;
    }
  });

  return qualifiedRoutes;
};

const defaultExport = AppConfig.gl2AppPathPrefix() ? qualifyUrls(Routes, AppConfig.gl2AppPathPrefix()) : Routes;

defaultExport.unqualified = Routes;

/*
 * Global registry of plugin routes. Route names are generated automatically from the route path, by removing
 * any colons, replacing slashes with underscores, and making the string uppercase. Below there is an example of how
 * to access the routes.
 *
 * Plugin register example:
 * routes: [
 *           { path: '/system/pipelines', component: Foo },
 *           { path: '/system/pipelines/:pipelineId', component: Bar },
 * ]
 *
 * Using routes on plugin components:
 * <LinkContainer to={Routes.pluginRoutes('SYSTEM_PIPELINES')}>...</LinkContainer>
 * <LinkContainer to={Routes.pluginRoutes('SYSTEM_PIPELINES_PIPELINEID')(123)}>...</LinkContainer>
 *
 */
defaultExport.pluginRoute = (routeKey, throwError = true) => {
  const pluginRoutes = {};

  PluginStore.exports('routes').forEach((pluginRoute) => {
    const uri = new URI(pluginRoute.path);
    const segments = uri.segment();
    const key = segments.map((segment) => segment.replace(':', '')).join('_').toUpperCase();
    const paramNames = segments.filter((segment) => segment.startsWith(':'));

    if (paramNames.length > 0) {
      pluginRoutes[key] = (...paramValues) => {
        paramNames.forEach((param, idx) => {
          const value = String(paramValues[idx]);

          uri.segment(segments.indexOf(param), value);
        });

        return uri.pathname();
      };

      return;
    }

    pluginRoutes[key] = pluginRoute.path;
  });

  const route = (AppConfig.gl2AppPathPrefix() ? qualifyUrls(pluginRoutes, AppConfig.gl2AppPathPrefix()) : pluginRoutes)[routeKey];

  if (!route && throwError) {
    throw new Error(`Could not find plugin route '${routeKey}'.`);
  }

  return route;
};

defaultExport.getPluginRoute = (routeKey) => {
  return defaultExport.pluginRoute(routeKey, false);
};

export default defaultExport;
