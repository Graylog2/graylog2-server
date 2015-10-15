const jsRoutes = {
  controllers: {
    api: {
      DashboardsApiController: {
        index: () => { return {url: '/dashboards' }; },
        delete: (id) => { return {url: '/dashboards/' + id }; },
      },
      StreamsApiController: {
        get: (streamId) => { return {url: '/streams/' + streamId }; },
      },
      InputsApiController: {
        list: () => { return {url: '/system/inputs'}; },
        globalRecentMessage: (inputId) => { return {url: '/' + inputId}; },
      },
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
  },
};

export default jsRoutes;
