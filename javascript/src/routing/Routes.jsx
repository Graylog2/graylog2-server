const Routes = {
  HOME: '/',
  STREAMS: '/streams',
  SOURCES: '/sources',
  DASHBOARDS: '/dashboards',
  SYSTEM: {
    OVERVIEW: '/system/overview',
    NODES: '/system/nodes',
  },
  USER_EDIT: '/user/edit',
  stream_edit: (streamId) => '/streams/' + streamId + '/edit',
  stream_outputs: (streamId) => '/streams/' + streamId + '/outputs',
  stream_alerts: (streamId) => '/streams/' + streamId + '/alerts',
  stream_search: (streamId, query, type, range) => '/streams/' + streamId + '/search' + '?query=' + query + "&type=" + range + '&range=' + range,
  startpage_set: (type, id) => '/startpage/set/' + type + '/' + id,

  dashboard_show: (dashboardId) => '/dashboards/' + dashboardId,
};

export default Routes;
