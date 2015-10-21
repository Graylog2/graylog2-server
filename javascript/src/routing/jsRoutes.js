const jsRoutes = {
  controllers: {
    api: {
      BundlesApiController: {
        list: () => { return {url: '/system/bundles'}; },
      },
      DashboardsApiController: {
        create: () => { return {url: '/dashboards' }; },
        index: () => { return {url: '/dashboards' }; },
        get: (id) => { return {url: '/dashboards/' + id }; },
        delete: (id) => { return {url: '/dashboards/' + id }; },
        update: (id) => { return {url: '/dashboards/' + id }; },
        addWidget: (id) => { return {url: '/dashboards/' + id + '/widgets'}; },
        widget: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId}; },
        updateWidget: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId}; },
        widgetValue: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId + '/value'}; },
      },
      InputsApiController: {
        list: () => { return {url: '/system/inputs'}; },
        globalRecentMessage: (inputId) => { return {url: '/' + inputId}; },
      },
      OutputsApiController: {
        index: () => { return {url: '/system/outputs'}; },
        create: () => { return {url: '/system/outputs'}; },
        delete: (outputId) => { return {url: '/system/outputs/' + outputId}; },
        update: (outputId) => { return {url: '/system/outputs/' + outputId}; },
        availableType: (type) => { return {url: '/system/outputs/available/' + type}; },
        availableTypes: () => { return {url: '/system/outputs/available'}; },
      },
      RolesApiController: {
        listRoles: () => { return {url: '/roles'}; },
        createRole: () => { return {url: '/roles'}; },
        updateRole: (rolename) => { return {url: '/roles/' + rolename}; },
        deleteRole: (rolename) => { return {url: '/roles/' + rolename}; },
        loadMembers: (rolename) => { return {url: '/roles/' + rolename + '/members'}; },
      },
      SavedSearchesApiController: {
        create: () => { return {url: '/search/saved'}; },
        delete: (savedSearchId) => { return {url: `/search/saved/${savedSearchId}`}; },
        list: () => { return {url: '/search/saved'}; },
        update: (savedSearchId) => { return {url: `/search/saved/${savedSearchId}`}; },
      },
      StreamsApiController: {
        get: (streamId) => { return {url: '/streams/' + streamId}; },
        create: () => { return {url: '/streams'}; },
        update: (streamId) => { return {url: '/streams/' + streamId}; },
        cloneStream: (streamId) => { return {url: '/streams/' + streamId + '/clone'}; },
        delete: (streamId) => { return {url: '/streams/' + streamId}; },
        pause: (streamId) => { return {url: '/streams/' + streamId + '/pause'}; },
        resume: (streamId) => { return {url: '/streams/' + streamId + '/resume'}; },
      },
      StreamOutputsApiController: {
        add: (streamId, outputId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        index: (streamId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        delete: (streamId, outputId) => { return {url: '/streams/' + streamId + '/outputs/' + outputId}; },
      },
      StreamRulesApiController: {
        delete: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        update: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        create: (streamId) => { return {url: '/streams/' + streamId + '/rules'}; },
      },
      SystemApiController: {
        fields: () => { return {url: '/system/fields'}; },
      },
      ToolsApiController: {
        naturalDateTest: (text) => { return {url: `/tools/natural_date_tester?string=${text}`}; },
      },
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
    MessagesController: {
      single: (index, messageId) => { return {url: `/messages/${index}/${messageId}`}; },
      analyze: (index, string) => { return {url: `/messages/${index}/analyze?string=${string}`}; },
    },
    SearchController: {
      index: () => { return {url: '/search'}; },
    },
  },
};

export default jsRoutes;
