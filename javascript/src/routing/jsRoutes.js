const jsRoutes = {
  controllers: {
    api: {
      AlarmCallbacksApiController: {
        available: (streamId) => { return {url: '/streams/' + streamId + '/alarmcallbacks/available'}; },
        create: (streamId) => { return {url: '/streams/' + streamId + '/alarmcallbacks'}; },
        delete: (streamId, alarmCallbackId) => { return {url: '/streams/' + streamId + '/alarmcallbacks/' + alarmCallbackId}; },
        list: (streamId) => { return {url: '/streams/' + streamId + '/alarmcallbacks'}; },
        update: (streamId, alarmCallbackId) => { return {url: '/streams/' + streamId + '/alarmcallbacks/' + alarmCallbackId}; },
      },
      AlarmCallbackHistoryApiController: {
        list: (streamId, alertId) => { return {url: '/streams/' + streamId + '/alerts/' + alertId + '/history'}; },
      },
      AlertsApiController: {
        list: (streamId, skip, limit) => { return {url: '/streams/' + streamId + '/alerts/paginated?skip=' + skip + '&limit=' + limit}; },
      },
      BundlesApiController: {
        apply: (bundleId) => { return {url: '/system/bundles/' + bundleId + '/apply'}; },
        create: () => { return {url: '/system/bundles'}; },
        delete: (bundleId) => { return {url: '/system/bundles/' + bundleId}; },
        export: () => { return {url: '/system/bundles/export'}; },
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
        get: (id) => { return {url: `/system/inputs/${id}`}; },
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
      UsersApiController: {
        changePassword: (username) => { return {url: '/users/' + username + '/password'}; },
        create: () => { return {url: '/users'}; },
        list: () => { return {url: '/users'}; },
        load: (username) => { return {url: '/users/' + username}; },
        delete: (username) => { return {url: '/users/' + username}; },
        update: (username) => { return {url: '/users/' + username}; },
        updateRoles: (username) => { return {url: '/users/' + username + '/roles'}; },
      },
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
    ExtractorsController: {
      newExtractor: (nodeId, inputId, extractorType, fieldName, index, messageId) => {
        return {url: `/system/inputs/${nodeId}/${inputId}/extractors/new?extractor_type=${extractorType}&field=${fieldName}&example_index=${index}&example_id=${messageId}`};
      },
    },
    MessagesController: {
      single: (index, messageId) => { return {url: `/messages/${index}/${messageId}`}; },
      analyze: (index, string) => { return {url: `/messages/${index}/analyze?string=${string}`}; },
    },
    NodesController: {
      node: (nodeId) => { return {url: `/system/nodes/${nodeId}`}; },
    },
    RadiosController: {
      show: (nodeId) => { return {url: `/system/radios/${nodeId}`}; },
    },
    StreamRulesController: {
      index: (streamId) => { return {url: `/streams/${streamId}/rules`}; },
    },
    SearchController: {
      index: (query, rangetype, timerange) => {
        let route;
        if (query && rangetype && timerange) {
          route = {url: `/search?q=${query}&${rangetype}=${timerange}`};
        } else {
          route = {url: '/search'};
        }

        return route;
      },
      showMessage: (index, messageId) => { return {url: `/messages/${index}/${messageId}`}; },
    },
  },
};

export default jsRoutes;
