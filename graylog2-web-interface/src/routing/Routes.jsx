import AppConfig from "util/AppConfig";
import {PluginStore} from "graylog-web-plugin/plugin";
import URI from "urijs";

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
const pluginRoutes = {};
PluginStore.exports('routes').forEach((pluginRoute) => {
  const uri = new URI(pluginRoute.path);
  const segments = uri.segment();
  const key = segments.map(segment => segment.replace(':', '')).join('_').toUpperCase();
  const paramNames = segments.filter(segment => segment.startsWith(':'));

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

const Routes = {
  STARTPAGE: '/',
  NOTFOUND: '/notfound',
  SEARCH: '/search',
  STREAMS: '/streams',
  ALERTS: {
    LIST: '/alerts',
    CONDITIONS: '/alerts/conditions',
    NEW_CONDITION: '/alerts/conditions/new',
    NOTIFICATIONS: '/alerts/notifications',
    NEW_NOTIFICATION: '/alerts/notifications/new',
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
      show: contentPackId => `/system/contentpacks/${contentPackId}`,
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
      SHOW: indexSetId => `/system/index_sets/${indexSetId}`,
      CREATE: '/system/index_sets/create',
    },
    INPUTS: '/system/inputs',
    LOGGING: '/system/logging',
    METRICS: nodeId => `/system/metrics/node/${nodeId}`,
    NODES: {
      LIST: '/system/nodes',
      SHOW: nodeId => `/system/nodes/${nodeId}`,
    },
    THREADDUMP: nodeId => `/system/threaddump/${nodeId}`,
    OUTPUTS: '/system/outputs',
    OVERVIEW: '/system/overview',
    AUTHENTICATION: {
      OVERVIEW: '/system/authentication',
      ROLES: '/system/authentication/roles',
      USERS: {
        CREATE: '/system/authentication/users/new',
        edit: username => `/system/authentication/users/edit/${username}`,
        TOKENS: {
          edit: username => `/system/authentication/users/tokens/${username}`,
        },
        LIST: '/system/authentication/users',
      },
      PROVIDERS: {
        CONFIG: '/system/authentication/config',
        provider: name => `/system/authentication/config/${name}`,
      },
    },
    LOOKUPTABLES: {
      OVERVIEW: '/system/lookuptables',
      CREATE: '/system/lookuptables/create',
      show: tableName => `/system/lookuptables/table/${tableName}`,
      edit: tableName => `/system/lookuptables/table/${tableName}/edit`,
      CACHES: {
        OVERVIEW: '/system/lookuptables/caches',
        CREATE: '/system/lookuptables/caches/create',
        show: cacheName => `/system/lookuptables/caches/${cacheName}`,
        edit: cacheName => `/system/lookuptables/caches/${cacheName}/edit`,
      },
      DATA_ADAPTERS: {
        OVERVIEW: '/system/lookuptables/data_adapters',
        CREATE: '/system/lookuptables/data_adapters/create',
        show: adapterName => `/system/lookuptables/data_adapter/${adapterName}`,
        edit: adapterName => `/system/lookuptables/data_adapter/${adapterName}/edit`,
      },
    },
    PIPELINES: {
      OVERVIEW: '/system/pipelines',
      PIPELINE: pipelineId => `/system/pipelines/${pipelineId}`,
      RULES: '/system/pipelines/rules',
      RULE: ruleId => `/system/pipelines/rules/${ruleId}`,
      SIMULATOR: '/system/pipelines/simulate',
    },
    ENTERPRISE: '/system/enterprise',
    SIDECARS: {
      OVERVIEW: '/system/sidecars',
      STATUS: sidecarId => `/system/sidecars/${sidecarId}/status`,
      ADMINISTRATION: '/system/sidecars/administration',
      CONFIGURATION: '/system/sidecars/configuration',
      NEW_CONFIGURATION: '/system/sidecars/configuration/new',
      EDIT_CONFIGURATION: configurationId => `/system/sidecars/configuration/edit/${configurationId}`,
      NEW_COLLECTOR: '/system/sidecars/collector/new',
      EDIT_COLLECTOR: collectorId => `/system/sidecars/collector/edit/${collectorId}`,
    },
  },
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
  stream_edit: streamId => `/streams/${streamId}/edit`,
  stream_edit_example: (streamId, index, messageId) => `${Routes.stream_edit(streamId)}?index=${index}&message_id=${messageId}`,
  stream_outputs: streamId => `/streams/${streamId}/outputs`,
  stream_search: (streamId, query, timeRange, resolution) => {
    return Routes._common_search_url(`${Routes.STREAMS}/${streamId}/search`, query, timeRange, resolution);
  },
  stream_alerts: streamId => `/streams/${streamId}/alerts`,

  legacy_stream_search: streamId => `/streams/${streamId}/messages`,
  show_alert: alertId => `${Routes.ALERTS.LIST}/${alertId}`,
  show_alert_condition: (streamId, conditionId) => `${Routes.ALERTS.CONDITIONS}/${streamId}/${conditionId}`,
  new_alert_condition_for_stream: streamId => `${Routes.ALERTS.NEW_CONDITION}?stream_id=${streamId}`,
  new_alert_notification_for_stream: streamId => `${Routes.ALERTS.NEW_NOTIFICATION}?stream_id=${streamId}`,

  dashboard_show: dashboardId => `/dashboards/${dashboardId}`,

  node: nodeId => `/system/nodes/${nodeId}`,

  node_inputs: nodeId => `${Routes.SYSTEM.INPUTS}/${nodeId}`,
  global_input_extractors: inputId => `/system/inputs/${inputId}/extractors`,
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
  getting_started: fromMenu => `${Routes.GETTING_STARTED}?menu=${fromMenu}`,
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
window.pluginRoutes = AppConfig.gl2AppPathPrefix() ? qualifyUrls(pluginRoutes, AppConfig.gl2AppPathPrefix()) : pluginRoutes;

// Plugin routes need to be prefixed separately, so we add them to the Routes object just at the end.
defaultExport.pluginRoute = (key) => {
  const route = window.pluginRoutes[key];
  if (!route) {
    console.error(`Could not find plugin route '${key}'.`);
  }

  return route;
};

export default defaultExport;
