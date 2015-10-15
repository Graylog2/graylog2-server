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
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
  },
};

export default jsRoutes;
