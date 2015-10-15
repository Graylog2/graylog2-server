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
        create: (streamId) => { return {url: '/streams'}; },
        update: (streamId) => { return {url: '/streams/' + streamId}; },
        cloneStream: (streamId) => { return {url: '/streams/' + streamId + '/clone'}; },
        delete: (streamId) => { return {url: '/streams/' + streamId}; },
        pause: (streamId) => { return {url: '/streams/' + streamId + '/pause'}; },
        resume: (streamId) => { return {url: '/streams/' + streamId + '/resume'}; },
      },
      StreamRulesApiController: {
        delete: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        update: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        create: (streamId) => { return {url: '/streams/' + streamId + '/rules'}; },
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
