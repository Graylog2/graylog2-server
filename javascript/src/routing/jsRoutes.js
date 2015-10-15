const jsRoutes = {
  controllers: {
    api: {
      DashboardsApiController: {
        index: () => { return {url: '/dashboards' }; },
        delete: (id) => { return {url: '/dashboards/' + id }; },
      },
      InputsApiController: {
        list: () => { return {url: '/system/inputs'}; },
        globalRecentMessage: (inputId) => { return {url: '/' + inputId}; },
      },
      StreamsApiController: {
        get: (streamId) => { return {url: '/streams/' + streamId}; },
        update: (streamId) => { return {url: '/streams/' + streamId}; },
      },
      StreamRulesApiController: {
        delete: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        update: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
      },
      SystemApiController: {
        fields: () => { return {url: '/system/fields'}; },
      }
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
  },
};

export default jsRoutes;
