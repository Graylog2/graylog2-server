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
import type { TimeRangeTypes } from 'views/logic/queries/Query';

type RoutesRelativeTimeRange = {
  relative: number
};
type RoutesAbsoluteTimeRange = {
  from: string,
  to: string,
};
type RoutesKeywordTimeRange = {
  keyword: string
};
type RoutesTimeRange = RoutesRelativeTimeRange | RoutesAbsoluteTimeRange | RoutesKeywordTimeRange;

const Routes = {
  STARTPAGE: '/',
  NOTFOUND: '/notfound',
  SEARCH: '/search',
  STREAMS: '/streams',
  ALERTS: {
    LIST: '/alerts',
    DEFINITIONS: {
      LIST: '/alerts/definitions',
      CREATE: '/alerts/definitions/new',
      edit: (definitionId: string) => `/alerts/definitions/${definitionId}/edit`,
      show: (definitionId: string) => `/alerts/definitions/${definitionId}`,
    },
    NOTIFICATIONS: {
      LIST: '/alerts/notifications',
      CREATE: '/alerts/notifications/new',
      edit: (notificationId: string) => `/alerts/notifications/${notificationId}/edit`,
      show: (notificationId: string) => `/alerts/notifications/${notificationId}`,
    },
  },
  SECURITY: '/security',
  SOURCES: '/sources',
  DASHBOARDS: '/dashboards',
  GETTING_STARTED: '/gettingstarted',
  GLOBAL_API_BROWSER_URL: '/api/api-browser/global/index.html',
  SYSTEM: {
    CONFIGURATIONS: '/system/configurations',
    CONTENTPACKS: {
      LIST: '/system/contentpacks',
      EXPORT: '/system/contentpacks/export',
      CREATE: '/system/contentpacks/create',
      edit: (contentPackId: string, contentPackRev: string) => `/system/contentpacks/${contentPackId}/${contentPackRev}/edit`,
      show: (contentPackId: string) => `/system/contentpacks/${contentPackId}`,
    },
    GROKPATTERNS: '/system/grokpatterns',
    INDICES: {
      LIST: '/system/indices',
      FAILURES: '/system/indices/failures',
    },
    INDEX_SETS: {
      CONFIGURATION: (indexSetId: string, from?: string) => {
        if (from) {
          return `/system/index_sets/${indexSetId}/configuration?from=${from}`;
        }

        return `/system/index_sets/${indexSetId}/configuration`;
      },
      SHOW: (indexSetId: string) => `/system/index_sets/${indexSetId}`,
      CREATE: '/system/index_sets/create',
    },
    INPUTS: '/system/inputs',
    LOGGING: '/system/logging',
    METRICS: (nodeId: string) => `/system/metrics/node/${nodeId}`,
    NODES: {
      LIST: '/system/nodes',
      SHOW: (nodeId: string) => `/system/nodes/${nodeId}`,
    },
    THREADDUMP: (nodeId: string) => `/system/threaddump/${nodeId}`,
    OUTPUTS: '/system/outputs',
    OVERVIEW: '/system/overview',
    PROCESSBUFFERDUMP: (nodeId: string) => `/system/processbufferdump/${nodeId}`,
    AUTHENTICATION: {
      BACKENDS: {
        OVERVIEW: '/system/authentication/services',
        ACTIVE: '/system/authentication/services/active',
        CREATE: '/system/authentication/services/create',
        createBackend: (name) => `/system/authentication/services/create/${name}`,
        show: (id: string) => `/system/authentication/services/${id}`,
        edit: (id: string, initialStepKey?: string) => {
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
      edit: (userId: string) => `/system/users/edit/${userId}`,
      TOKENS: {
        edit: (userId: string) => `/system/users/tokens/${userId}`,
      },
      OVERVIEW: '/system/users',
      show: (userId: string) => `/system/users/${userId}`,
    },
    AUTHZROLES: {
      OVERVIEW: '/system/roles',
      show: (roleId: string) => `/system/roles/${roleId}`,
      edit: (roleId: string) => `/system/roles/edit/${roleId}`,
    },
    LOOKUPTABLES: {
      OVERVIEW: '/system/lookuptables',
      CREATE: '/system/lookuptables/create',
      show: (tableName: string) => `/system/lookuptables/table/${tableName}`,
      edit: (tableName: string) => `/system/lookuptables/table/${tableName}/edit`,
      CACHES: {
        OVERVIEW: '/system/lookuptables/caches',
        CREATE: '/system/lookuptables/caches/create',
        show: (cacheName: string) => `/system/lookuptables/caches/${cacheName}`,
        edit: (cacheName: string) => `/system/lookuptables/caches/${cacheName}/edit`,
      },
      DATA_ADAPTERS: {
        OVERVIEW: '/system/lookuptables/data_adapters',
        CREATE: '/system/lookuptables/data_adapters/create',
        show: (adapterName: string) => `/system/lookuptables/data_adapter/${adapterName}`,
        edit: (adapterName: string) => `/system/lookuptables/data_adapter/${adapterName}/edit`,
      },
    },
    PIPELINES: {
      OVERVIEW: '/system/pipelines',
      PIPELINE: (pipelineId: string) => `/system/pipelines/${pipelineId}`,
      RULES: '/system/pipelines/rules',
      RULE: (ruleId: string) => `/system/pipelines/rules/${ruleId}`,
      SIMULATOR: '/system/pipelines/simulate',
    },
    ENTERPRISE: '/system/enterprise',
    SIDECARS: {
      OVERVIEW: '/system/sidecars',
      STATUS: (sidecarId: string) => `/system/sidecars/${sidecarId}/status`,
      ADMINISTRATION: '/system/sidecars/administration',
      CONFIGURATION: '/system/sidecars/configuration',
      NEW_CONFIGURATION: '/system/sidecars/configuration/new',
      EDIT_CONFIGURATION: (configurationId: string) => `/system/sidecars/configuration/edit/${configurationId}`,
      NEW_COLLECTOR: '/system/sidecars/collector/new',
      EDIT_COLLECTOR: (collectorId: string) => `/system/sidecars/collector/edit/${collectorId}`,
    },
  },
  VIEWS: {
    LIST: viewsPath,
    VIEWID: (id: string) => `${viewsPath}/${id}`,
  },
  EXTENDEDSEARCH: extendedSearchPath,
  search_with_query: (query: string, rangeType: TimeRangeTypes, timeRange: RoutesTimeRange) => {
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
  _common_search_url: (resource: string, query: string | undefined, timeRange: RoutesTimeRange | undefined, resolution: number | undefined) => {
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

    route.setQuery(queryParams);

    return route.resource();
  },
  search: (query: string, timeRange: RoutesTimeRange, resolution?: number) => {
    return Routes._common_search_url(Routes.SEARCH, query, timeRange, resolution);
  },
  message_show: (index: string, messageId: string) => `/messages/${index}/${messageId}`,
  stream_edit: (streamId: string) => `/streams/${streamId}/edit`,
  stream_edit_example: (streamId: string, index: string, messageId: string) => `${Routes.stream_edit(streamId)}?index=${index}&message_id=${messageId}`,
  stream_outputs: (streamId: string) => `/streams/${streamId}/outputs`,
  stream_search: (streamId: string, query?: string, timeRange?: RoutesTimeRange, resolution?: number) => {
    return Routes._common_search_url(`${Routes.STREAMS}/${streamId}/search`, query, timeRange, resolution);
  },
  stream_alerts: (streamId: string) => `/alerts/?stream_id=${streamId}`,

  legacy_stream_search: (streamId: string) => `/streams/${streamId}/messages`,

  dashboard_show: (dashboardId: string) => `/dashboards/${dashboardId}`,

  node: (nodeId: string) => `/system/nodes/${nodeId}`,

  node_inputs: (nodeId: string) => `${Routes.SYSTEM.INPUTS}/${nodeId}`,
  global_input_extractors: (inputId: string) => `/system/inputs/${inputId}/extractors`,
  local_input_extractors: (nodeId: string, inputId: string) => `/system/inputs/${nodeId}/${inputId}/extractors`,
  export_extractors: (nodeId: string, inputId: string) => `${Routes.local_input_extractors(nodeId, inputId)}/export`,
  import_extractors: (nodeId: string, inputId: string) => `${Routes.local_input_extractors(nodeId, inputId)}/import`,
  new_extractor: (nodeId: string, inputId: string, extractorType?: string, fieldName?: string, index?: string, messageId?: string) => {
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
  edit_extractor: (nodeId: string, inputId: string, extractorId: string) => `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,

  edit_input_extractor: (nodeId: string, inputId: string, extractorId: string) => `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,
  getting_started: (fromMenu) => `${Routes.GETTING_STARTED}?menu=${fromMenu}`,
  filtered_metrics: (nodeId: string, filter: string) => `${Routes.SYSTEM.METRICS(nodeId)}?filter=${filter}`,
  global_api_browser: () => Routes.GLOBAL_API_BROWSER_URL,
} as const;

const prefixUrlWithoutHostname = (url: string, prefix: string) => {
  const uri = new URI(url);

  return uri.directory(`${prefix}/${uri.directory()}`)
    .normalizePath()
    .resource();
};

type RouteFunction<P extends Array<any>> = (...args: P) => string;
type RouteMapEntry = string | RouteFunction<any> | RouteMap;
type RouteMap = { [routeName: string]: RouteMapEntry };

const isLiteralRoute = (entry: RouteMapEntry): entry is string => (typeof entry === 'string');
const isRouteFunction = (entry: RouteMapEntry): entry is RouteFunction<any> => (typeof entry === 'function');

const qualifyUrls = <R extends RouteMap>(routes: R, appPrefix: string): R => {
  if (appPrefix === '/') {
    return routes;
  }

  return Object.fromEntries(Object.entries(routes).map(([routeName, routeValue]) => {
    if (isLiteralRoute(routeValue)) {
      return [routeName, prefixUrlWithoutHostname(routeValue, appPrefix)];
    }

    if (isRouteFunction(routeValue)) {
      return [routeName, (...params: Parameters<typeof routeValue>) => {
        const result = routeValue(...params);

        return prefixUrlWithoutHostname(result, appPrefix);
      }];
    }

    return [routeName, qualifyUrls(routeValue, appPrefix)];
  }));
};

const qualifiedRoutes: typeof Routes = AppConfig.gl2AppPathPrefix() ? qualifyUrls(Routes, AppConfig.gl2AppPathPrefix()) : Routes;

const unqualified = Routes;

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
const pluginRoute = (routeKey: string, throwError: boolean = true) => {
  const pluginRoutes = {};

  PluginStore.exports('routes').forEach((route) => {
    const uri = new URI(route.path);
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

    pluginRoutes[key] = route.path;
  });

  const route = (AppConfig.gl2AppPathPrefix() ? qualifyUrls(pluginRoutes, AppConfig.gl2AppPathPrefix()) : pluginRoutes)[routeKey];

  if (!route && throwError) {
    throw new Error(`Could not find plugin route '${routeKey}'.`);
  }

  return route;
};

const getPluginRoute = (routeKey: string) => pluginRoute(routeKey, false);

/**
 * Exported constants for using strings to check if a plugin is registered by it's description.
 *
 */
export const ENTERPRISE_ROUTE_DESCRIPTION = 'Enterprise';
export const SECURITY_ROUTE_DESCRIPTION = 'Security';

const defaultExport = Object.assign(qualifiedRoutes, { pluginRoute, getPluginRoute, unqualified });

export default defaultExport;
