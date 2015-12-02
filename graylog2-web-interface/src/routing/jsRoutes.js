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
      CountsApiController: {
        total: () => { return {url: '/count/total'}; },
      },
      DashboardsApiController: {
        create: () => { return {url: '/dashboards' }; },
        index: () => { return {url: '/dashboards' }; },
        get: (id) => { return {url: '/dashboards/' + id }; },
        delete: (id) => { return {url: '/dashboards/' + id }; },
        update: (id) => { return {url: '/dashboards/' + id }; },
        addWidget: (id) => { return {url: '/dashboards/' + id + '/widgets'}; },
        removeWidget: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId}; },
        widget: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId}; },
        updateWidget: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId}; },
        widgetValue: (dashboardId, widgetId) => { return {url: '/dashboards/' + dashboardId + '/widgets/' + widgetId + '/value'}; },
        updatePositions: (dashboardId) => { return {url: '/dashboards/' + dashboardId + '/positions'}; },
      },
      DeflectorApiController: {
        config: () => { return {url: '/system/deflector/config'}; },
        cycle: () => { return {url: '/system/deflector/cycle'}; },
        list: () => { return {url: '/system/deflector'}; },
      },
      IndexerClusterApiController: {
        health: () => { return {url: '/system/indexer/cluster/health'}; },
        name: () => { return {url: '/system/indexer/cluster/name'}; },
      },
      IndexerFailuresApiController: {
        count: (since) => { return {url: '/system/indexer/failures/count?since=' + since}; },
        list: (limit, offset) => { return {url: '/system/indexer/failures?limit=' + limit + '&offset=' + offset}; },
      },
      IndexRangesApiController: {
        list: () => { return {url: '/system/indices/ranges'}; },
        rebuild: () => { return {url: '/system/indices/ranges/rebuild'}; },
      },
      IndicesApiController: {
        close: (indexName) => { return {url: '/system/indexer/indices/' + indexName + '/close'}; },
        delete: (indexName) => { return {url: '/system/indexer/indices/' + indexName}; },
        list: () => { return {url: '/system/indexer/indices/all'}; },
        listClosed: () => { return {url: '/system/indexer/indices/closed'}; },
        reopen: (indexName) => { return {url: '/system/indexer/indices/' + indexName + '/reopen'}; },
      },
      InputsApiController: {
        list: () => { return {url: '/system/inputs'}; },
        get: (id) => { return {url: `/system/inputs/${id}`}; },
        globalRecentMessage: (inputId) => { return {url: '/' + inputId}; },
      },
      MessageFieldsApiController: {
        list: () => { return {url: '/system/fields'}; },
      },
      MetricsApiController: {
        multiple: () => { return {url: '/system/metrics/multiple'}; },
        byNamespace: (namespace) => { return {url: '/system/metrics/namespace/' + namespace}; },
      },
      ClusterMetricsApiController: {
        multiple: (nodeId) => { return {url: '/cluster/' + nodeId + '/metrics/multiple'}; },
        byNamespace: (nodeId, namespace) => { return {url: '/cluster/' + nodeId + '/metrics/namespace/' + namespace}; },
      },
      NotificationsApiController: {
        delete: (type) => { return {url: '/system/notifications/' + type}; },
        list: () => { return {url: '/system/notifications'}; },
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
        update: (savedSearchId) => { return {url: `/search/saved/${savedSearchId}`}; },
      },
      StreamAlertsApiController: {
        create: (streamId) => { return {url: '/streams/' + streamId + '/alerts/conditions'}; },
        delete: (streamId, alertConditionId) => { return {url: '/streams/' + streamId + '/alerts/conditions/' + alertConditionId}; },
        list: (streamId) => { return {url: '/streams/' + streamId + '/alerts/conditions'}; },
        update: (streamId, alertConditionId) => { return {url: '/streams/' + streamId + '/alerts/conditions/' + alertConditionId}; },
        addReceiver: (streamId, type, entity) => { return {url: '/streams/' + streamId + '/alerts/receivers?entity=' + entity + '&type=' + type}; },
        deleteReceiver: (streamId, type, entity) => { return {url: '/streams/' + streamId + '/alerts/receivers?entity=' + entity + '&type=' + type}; },
        sendDummyAlert: (streamId) => { return {url: '/streams/' + streamId + '/alerts/sendDummyAlert'}; },
      },
      StreamsApiController: {
        get: (streamId) => { return {url: '/streams/' + streamId}; },
        create: () => { return {url: '/streams'}; },
        update: (streamId) => { return {url: '/streams/' + streamId}; },
        cloneStream: (streamId) => { return {url: '/streams/' + streamId + '/clone'}; },
        delete: (streamId) => { return {url: '/streams/' + streamId}; },
        pause: (streamId) => { return {url: '/streams/' + streamId + '/pause'}; },
        resume: (streamId) => { return {url: '/streams/' + streamId + '/resume'}; },
        testMatch: (streamId) => { return {url: '/streams/' + streamId + '/testMatch'}; },
      },
      StreamOutputsApiController: {
        add: (streamId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        index: (streamId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        delete: (streamId, outputId) => { return {url: '/streams/' + streamId + '/outputs/' + outputId}; },
      },
      StreamRulesApiController: {
        delete: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        update: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        create: (streamId) => { return {url: '/streams/' + streamId + '/rules'}; },
      },
      SystemApiController: {
        info: () => { return {url: '/system'}; },
        fields: () => { return {url: '/system/fields'}; },
      },
      SystemJobsApiController: {
        list: () => { return {url: '/cluster/jobs'}; },
      },
      SystemMessagesApiController: {
        all: (page) => { return {url: '/system/messages?page=' + page}; },
      },
      ToolsApiController: {
        grokTest: () => { return {url: '/tools/grok_tester'};},
        jsonTest: () => { return {url: '/tools/json_tester'};},
        naturalDateTest: (text) => { return {url: `/tools/natural_date_tester?string=${text}`}; },
        regexTest: () => { return {url: '/tools/regex_tester'};},
        regexReplaceTest: () => { return {url: '/tools/regex_replace_tester'};},
        splitAndIndexTest: () => { return {url: '/tools/split_and_index_tester'};},
        substringTest: () => { return {url: '/tools/substring_tester'};},
      },
      UniversalSearchApiController: {
        search: (type, query, timerange, limit) => {
          let url = `/search/universal/${type}?query=${query}&${timerange}`;
          if (limit) {
            url += `&limit=${limit}`;
          }

          return {url: url};
        },
        histogram: (type, query, interval, timerange) => { return {url: `/search/universal/${type}/histogram?query=${query}&interval=${interval}&${timerange}`}; },
        fieldHistogram: (type, query, field, resolution, timerange) => { return {url: `/search/universal/${type}/fieldhistogram?query=${query}&interval=${resolution}&field=${field}&${timerange}`}; },
        fieldStats: (type, query, field, timerange) => { return {url: `/search/universal/${type}/stats?query=${query}&field=${field}&${timerange}`}; },
        fieldTerms: (type, query, field, timerange) => { return {url: `/search/universal/${type}/terms?query=${query}&field=${field}&${timerange}`}; },
      },
      UsageStatsApiController: {
        pluginEnabled: () => { return {url: '/plugins/org.graylog.plugins.usagestatistics/config'}; },
        setOptOutState: () => { return {url: '/plugins/org.graylog.plugins.usagestatistics/opt-out'}; },
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
      create: (inputId) => { return {url: `/system/inputs/${inputId}/extractors`}; },
      delete: (inputId, extractorId) => { return {url: `/system/inputs/${inputId}/extractors/${extractorId}`}; },
      newExtractor: (nodeId, inputId, extractorType, fieldName, index, messageId) => {
        return {url: `/system/inputs/${nodeId}/${inputId}/extractors/new?extractor_type=${extractorType}&field=${fieldName}&example_index=${index}&example_id=${messageId}`};
      },
      order: (inputId) => { return {url: `/system/inputs/${inputId}/extractors/order`}; },
      update: (inputId, extractorId) => { return {url: `/system/inputs/${inputId}/extractors/${extractorId}`}; },
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
