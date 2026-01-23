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
import URI from 'urijs';

import AppConfig from 'util/AppConfig';
import {
  extendedSearchPath,
  viewsPath,
  dashboardsTvPath,
  showDashboardsPath,
  newDashboardsPath,
  dashboardsPath,
} from 'views/Constants';
import type { TimeRangeTypes } from 'views/logic/queries/Query';

export const SECURITY_PATH = '/security';

type RoutesRelativeTimeRange = {
  relative: number;
};
type RoutesAbsoluteTimeRange = {
  from: string;
  to: string;
};
type RoutesKeywordTimeRange = {
  keyword: string;
};
type RoutesTimeRange = RoutesRelativeTimeRange | RoutesAbsoluteTimeRange | RoutesKeywordTimeRange;
type SearchQueryParams = {
  q: string;
  relative?: number;
  rangetype?: string;
  from?: string;
  to?: string;
  keyword?: string;
  streams?: string;
  stream_categories?: string;
};

const Routes = {
  STARTPAGE: '/',
  NOTFOUND: '/notfound',
  SEARCH: '/search',
  SEARCH_SHOW: (id: string) => `/search/${id}`,
  STREAMS: '/streams',
  STREAM_NEW: '/streams/new',
  ALERTS: {
    LIST: '/alerts',
    replay_search: (alertId: string) => `/alerts/${alertId}/replay-search`,
    BULK_REPLAY_SEARCH: '/alerts/replay-search',
    DEFINITIONS: {
      LIST: '/alerts/definitions',
      CREATE: '/alerts/definitions/new',
      edit: (definitionId: string) => `/alerts/definitions/${definitionId}/edit`,
      show: (definitionId: string) => `/alerts/definitions/${definitionId}`,
      replay_search: (definitionId: string) => `/alerts/definitions/${definitionId}/replay-search`,
    },
    NOTIFICATIONS: {
      LIST: '/alerts/notifications',
      CREATE: '/alerts/notifications/new',
      edit: (notificationId: string) => `/alerts/notifications/${notificationId}/edit`,
      show: (notificationId: string) => `/alerts/notifications/${notificationId}`,
    },
  },
  SECURITY: {
    OVERVIEW: `${SECURITY_PATH}`,
    USER_ACTIVITY: `${SECURITY_PATH}/user-activity`,
    HOST_ACTIVITY: `${SECURITY_PATH}/host-activity`,
    NETWORK_ACTIVITY: `${SECURITY_PATH}/network-activity`,
    ANOMALIES: `${SECURITY_PATH}/anomalies`,
    ACTIVITY: `${SECURITY_PATH}/activity`,
  },
  SOURCES: '/sources',
  DASHBOARDS: dashboardsPath,
  DASHBOARD: {
    NEW: newDashboardsPath,
    SHOW: showDashboardsPath,
    FULL_SCREEN: dashboardsTvPath,
  },
  WELCOME: '/welcome',
  API_BROWSER: '/api-browser',
  SYSTEM: {
    CLUSTER: {
      NODES: '/system/cluster',
      NODE_SHOW: (nodeId: string) => `/system/cluster/node/${nodeId}`,
      CERTIFICATE_MANAGEMENT: '/system/cluster/certificate-management',
      DATANODE_DASHBOARD: '/system/cluster/datanode-dashboard',
      DATANODE_MIGRATION: '/system/cluster/datanode-migration',
      DATANODE_UPGRADE: '/system/cluster/datanode-upgrade',
      DATANODE_SHOW: (dataNodeId: string) => `/system/cluster/datanode/${dataNodeId}`,
    },
    CONFIGURATIONS: '/system/configurations',
    configurationsSection: (section: string, pluginSection?: string) =>
      `/system/configurations/${section}${pluginSection ? `/${pluginSection}` : ''}`,
    CONTENTPACKS: {
      LIST: '/system/contentpacks',
      EXPORT: '/system/contentpacks/export',
      CREATE: '/system/contentpacks/create',
      edit: (contentPackId: string, contentPackRev: string) =>
        `/system/contentpacks/${contentPackId}/${contentPackRev}/edit`,
      show: (contentPackId: string) => `/system/contentpacks/${contentPackId}`,
    },
    GROKPATTERNS: '/system/grokpatterns',
    INDICES: {
      LIST: '/system/indices',
      FAILURES: '/system/indices/failures',
      TEMPLATES: {
        view: (templateId: string) => `/system/indices/templates/${templateId}`,
        OVERVIEW: '/system/indices/templates',
        CREATE: '/system/indices/templates/create',
        edit: (templateId: string) => `/system/indices/templates/edit/${templateId}`,
      },
      FIELD_TYPE_PROFILES: {
        OVERVIEW: '/system/indices/field-type-profiles',
        edit: (profileId: string) => `/system/indices/field-type-profiles/${profileId}`,
        CREATE: '/system/indices/field-type-profiles/create',
      },
    },
    INDEX_SETS: {
      CONFIGURATION: (indexSetId: string, from?: string) => {
        if (from) {
          return `/system/index_sets/${indexSetId}/configuration?from=${from}`;
        }

        return `/system/index_sets/${indexSetId}/configuration`;
      },
      SHOW: (indexSetId: string) => `/system/index_sets/${indexSetId}`,
      FIELD_TYPES: (indexSetId: string) => `/system/index_sets/${indexSetId}/field-types`,
      CREATE: '/system/index_sets/create',
    },
    INPUTS: '/system/inputs',
    INPUT_DIAGNOSIS: (input: string) => `/system/input/diagnosis/${input}`,
    LOGGING: '/system/logging',
    METRICS: (nodeId: string) => `/system/metrics/node/${nodeId}`,
    THREADDUMP: (nodeId: string) => `/system/threaddump/${nodeId}`,
    OUTPUTS: '/system/outputs',
    OVERVIEW: '/system/overview',
    PROCESSBUFFERDUMP: (nodeId: string) => `/system/processbufferdump/${nodeId}`,
    SYSTEMLOGS: (nodeId: string) => `/system/logs/recent/${nodeId}`,
    AUTHENTICATION: {
      BACKENDS: {
        OVERVIEW: '/system/authentication/services',
        ACTIVE: '/system/authentication/services/active',
        CREATE: '/system/authentication/services/create',
        createBackend: (name: string) => `/system/authentication/services/create/${name}`,
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
    USERS_TOKEN_MANAGEMENT: {
      overview: '/system/tokenmanagement/overview',
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
      CREATE: '/system/pipelines/new',
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
      FAILURE_TRACKING: '/system/sidecars/failuretracking',
      NEW_CONFIGURATION: '/system/sidecars/configuration/new',
      EDIT_CONFIGURATION: (configurationId: string) => `/system/sidecars/configuration/edit/${configurationId}`,
      NEW_COLLECTOR: '/system/sidecars/collector/new',
      EDIT_COLLECTOR: (collectorId: string) => `/system/sidecars/collector/edit/${collectorId}`,
    },
    COLLECTORS: {
      OVERVIEW: '/system/collectors',
      FLEETS: '/system/collectors/fleets',
      FLEET: (fleetId: string) => `/system/collectors/fleets/${fleetId}`,
      INSTANCES: '/system/collectors/instances',
      INSTANCE: (instanceId: string) => `/system/collectors/instances/${instanceId}`,
      DEPLOYMENT: '/system/collectors/deployment',
    },
  },
  VIEWS: {
    LIST: viewsPath,
    VIEWID: (id: string) => `${viewsPath}/${id}`,
  },
  EXTENDEDSEARCH: extendedSearchPath,
  KEYBOARD_SHORTCUTS: '/keyboard-shortcuts',
  search_with_query: (
    query: string,
    rangeType: TimeRangeTypes,
    timeRange: RoutesTimeRange,
    streams?: string[],
    streamCategories?: string[],
  ) => {
    const route = new URI(Routes.SEARCH);
    const queryParams: SearchQueryParams = {
      q: query,
    };

    if (rangeType && timeRange) {
      queryParams.rangetype = rangeType;

      switch (rangeType) {
        case 'relative':
          queryParams.relative = (<RoutesRelativeTimeRange>timeRange).relative;
          break;
        case 'absolute':
          queryParams.from = (<RoutesAbsoluteTimeRange>timeRange).from;
          queryParams.to = (<RoutesAbsoluteTimeRange>timeRange).to;
          break;
        case 'keyword':
          queryParams.keyword = (<RoutesKeywordTimeRange>timeRange).keyword;
          break;
        default:
          throw new Error(`Invalid range type: ${rangeType}.`);
      }
    }

    if (streams) {
      queryParams.streams = streams.join(',');
    }

    if (streamCategories) {
      queryParams.stream_categories = streamCategories.join(',');
    }

    route.query(queryParams);

    return route.resource();
  },
  _common_search_url: (
    resource: string,
    query: string | undefined,
    timeRange: RoutesTimeRange | undefined,
    resolution: number | undefined,
  ) => {
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
  search: (query: string, timeRange: RoutesTimeRange, resolution?: number) =>
    Routes._common_search_url(Routes.SEARCH, query, timeRange, resolution),
  message_show: (index: string, messageId: string) => `/messages/${index}/${messageId}`,
  stream_view: (streamId: string) => `/streams/${streamId}/view`,
  stream_edit: (streamId: string) => `/streams/${streamId}/edit`,
  stream_edit_example: (streamId: string, index: string, messageId: string) =>
    `${Routes.stream_edit(streamId)}?index=${index}&message_id=${messageId}`,
  stream_outputs: (streamId: string) => `/streams/${streamId}/outputs`,
  stream_search: (streamId: string, query?: string, timeRange?: RoutesTimeRange, resolution?: number) =>
    Routes._common_search_url(`${Routes.STREAMS}/${streamId}/search`, query, timeRange, resolution),
  stream_alerts: (streamId: string) => `/alerts/?stream_id=${streamId}`,

  dashboard_show: (dashboardId: string) => `/dashboards/${dashboardId}`,

  show_saved_search: (searchId: string) => `/search/${searchId}`,

  node_inputs: (nodeId: string) => `${Routes.SYSTEM.INPUTS}/${nodeId}`,
  global_input_extractors: (inputId: string) => `/system/inputs/${inputId}/extractors`,
  local_input_extractors: (nodeId: string, inputId: string) => `/system/inputs/${nodeId}/${inputId}/extractors`,
  export_extractors: (nodeId: string, inputId: string) => `${Routes.local_input_extractors(nodeId, inputId)}/export`,
  import_extractors: (nodeId: string, inputId: string) => `${Routes.local_input_extractors(nodeId, inputId)}/import`,
  new_extractor: (
    nodeId: string,
    inputId: string,
    extractorType?: string,
    fieldName?: string,
    index?: string,
    messageId?: string,
  ) => {
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
  edit_extractor: (nodeId: string, inputId: string, extractorId: string) =>
    `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,

  edit_input_extractor: (nodeId: string, inputId: string, extractorId: string) =>
    `/system/inputs/${nodeId}/${inputId}/extractors/${extractorId}/edit`,
  filtered_metrics: (nodeId: string, filter: string) => `${Routes.SYSTEM.METRICS(nodeId)}?filter=${filter}`,
} as const;

const prefixUrlWithoutHostname = (url: string, prefix: string) => {
  const uri = new URI(url);

  return uri.directory(`${prefix}/${uri.directory()}`).normalizePath().resource();
};

type RouteFunction<P extends Array<any>> = (...args: P) => string;
type RouteMapEntry = string | RouteFunction<any> | RouteMap;
type RouteMap = { [routeName: string]: RouteMapEntry };

const isLiteralRoute = (entry: RouteMapEntry): entry is string => typeof entry === 'string';
const isRouteFunction = (entry: RouteMapEntry): entry is RouteFunction<any> => typeof entry === 'function';

declare const __brand: unique symbol;
type Brand<B> = { [__brand]: B };
export type Branded<T, B> = T & Brand<B>;

export type QualifiedUrl<T extends string> = Branded<T, 'Qualified URL'>;
type QualifiedFunction<F extends (...args: Parameters<F>) => string> = (...args: Parameters<F>) => QualifiedUrl<string>;

type QualifiedRoutes<T> = {
  [K in keyof T]: T[K] extends string
    ? QualifiedUrl<T[K]>
    : T[K] extends (...args: any[]) => string
      ? QualifiedFunction<T[K]>
      : T[K] extends object
        ? QualifiedRoutes<T[K]>
        : never;
};

export const qualifyUrls = <R extends RouteMap>(
  routes: R,
  appPrefix: string = AppConfig.gl2AppPathPrefix(),
): QualifiedRoutes<R> => {
  if (!appPrefix || appPrefix === '' || appPrefix === '/') {
    return routes as QualifiedRoutes<R>;
  }

  return Object.fromEntries(
    Object.entries(routes).map(([routeName, routeValue]) => {
      if (isLiteralRoute(routeValue)) {
        return [routeName, prefixUrlWithoutHostname(routeValue, appPrefix)];
      }

      if (isRouteFunction(routeValue)) {
        return [
          routeName,
          (...params: Parameters<typeof routeValue>) => {
            const result = routeValue(...params);

            return prefixUrlWithoutHostname(result, appPrefix);
          },
        ];
      }

      return [routeName, qualifyUrls(routeValue, appPrefix)];
    }),
  );
};

export const prefixUrl = <T extends string>(route: T): QualifiedUrl<T> => {
  const appPrefix = AppConfig.gl2AppPathPrefix();

  return (
    !appPrefix || appPrefix === '' || appPrefix === '/' ? route : prefixUrlWithoutHostname(route, appPrefix)
  ) as QualifiedUrl<T>;
};

const qualifiedRoutes = qualifyUrls(Routes);

const unqualified = Routes;

const defaultExport = {
  ...qualifiedRoutes,
  unqualified,
};

export default defaultExport;
