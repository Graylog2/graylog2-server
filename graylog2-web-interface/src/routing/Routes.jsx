import AppConfig from 'util/AppConfig';
import { PluginStore } from 'graylog-web-plugin/plugin';
import URI from 'urijs';

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
PluginStore.exports('routes').forEach(pluginRoute => {
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
  SEARCH: '/search',
  STREAMS: '/streams',
  SOURCES: '/sources',
  DASHBOARDS: '/dashboards',
  GETTING_STARTED: '/gettingstarted',
  SYSTEM: {
    CONFIGURATIONS: '/system/configurations',
    CONTENTPACKS: {
      LIST: '/system/contentpacks',
      EXPORT: '/system/contentpacks/export',
    },
    GROKPATTERNS: '/system/grokpatterns',
    INDICES: {
      LIST: '/system/indices',
      FAILURES: '/system/indices/failures',
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
    LDAP: {
      SETTINGS: '/system/ldap',
      GROUPS: '/system/ldap/groups',
    },
    AUTHENTICATION: {
      OVERVIEW: '/system/authentication',
      ROLES: '/system/authentication/roles',
      USERS: {
        CREATE: '/system/authentication/users/new',
        edit: (username) => `/system/authentication/users/edit/${username}`,
        LIST: '/system/authentication/users',
      },
      PROVIDERS: {
        CONFIG: '/system/authentication/config',
        provider: (name) => `/system/authentication/config/${name}`,
      },
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
  message_show: (index, messageId) => `/messages/${index}/${messageId}`,
  stream_edit: (streamId) => `/streams/${streamId}/edit`,
  stream_edit_example: (streamId, index, messageId) => `${Routes.stream_edit(streamId)}?index=${index}&message_id=${messageId}`,
  stream_outputs: (streamId) => `/streams/${streamId}/outputs`,
  stream_alerts: (streamId) => `/streams/${streamId}/alerts`,
  stream_search: (streamId) => `/streams/${streamId}/search`,
  legacy_stream_search: (streamId) => `/streams/${streamId}/messages`,

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
        qualifiedRoutes[routeName] = `${appPrefix}${routes[routeName]}`;
        break;
      case 'function':
        qualifiedRoutes[routeName] = (...params) => {
          const result = routes[routeName](...params);
          return `${appPrefix}${result}`;
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
