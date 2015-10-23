const Routes = {
  HOME: '/',
  SEARCH: '/search',
  STREAMS: '/streams',
  SOURCES: '/sources',
  DASHBOARDS: '/dashboards',
  SYSTEM: {
    COLLECTORS: '/system/collectors',
    CONTENTPACKS: {
      LIST: '/system/contentpacks',
      EXPORT: '/system/contentpacks/export',
    },
    GROKPATTERNS: '/system/grokpatterns',
    INPUTS: '/system/inputs',
    NODES: '/system/nodes',
    OUTPUTS: '/system/outputs',
    OVERVIEW: '/system/overview',
    ROLES: '/system/roles',
    USERS: {
      LIST: '/system/users',
      edit: (username) => '/system/users/edit/' + username,
    },
  },
  message_show: (index, messageId) => `/messages/${index}/${messageId}`,
  stream_edit: (streamId) => '/streams/' + streamId + '/edit',
  stream_outputs: (streamId) => '/streams/' + streamId + '/outputs',
  stream_alerts: (streamId) => '/streams/' + streamId + '/alerts',
  stream_search: (streamId, query, type, range) => '/streams/' + streamId + '/search' + '?query=' + query + '&type=' + range + '&range=' + range,
  startpage_set: (type, id) => '/startpage/set/' + type + '/' + id,

  dashboard_show: (dashboardId) => '/dashboards/' + dashboardId,

  global_input_extractors: (inputId) => `/system/inputs/${inputId}/extractors`,
  local_input_extractors: (nodeId, inputId) => `/system/inputs/${nodeId}/${inputId}/extractors`,
};

export default Routes;
