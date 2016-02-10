class StoreProvider {
  constructor() {
    /* eslint-disable import/no-require */
    this.stores = {
      AlarmCallbackHistory: () => require('stores/alarmcallbacks/AlarmCallbackHistoryStore'),
      AlarmCallbacks: () => require('stores/alarmcallbacks/AlarmCallbacksStore'),
      AlertConditions: () => require('stores/alertconditions/AlertConditionsStore'),
      Alerts: () => require('stores/alerts/AlertsStore'),
      ClusterOverview: () => require('stores/cluster/ClusterOverviewStore'),
      Collectors: () => require('stores/collectors/CollectorsStore'),
      ConfigurationBundles: () => require('stores/configuration-bundles/ConfigurationBundlesStore'),
      CurrentUser: () => require('stores/users/CurrentUserStore'),
      Dashboards: () => require('stores/dashboards/DashboardsStore'),
      Deflector: () => require('stores/indices/DeflectorStore'),
      Extractors: () => require('stores/extractors/ExtractorsStore'),
      FieldGraphs: () => require('stores/field-analyzers/FieldGraphsStore'),
      FieldQuickValues: () => require('stores/field-analyzers/FieldQuickValuesStore'),
      Fields: () => require('stores/fields/FieldsStore'),
      FieldStatistics: () => require('stores/field-analyzers/FieldStatisticsStore'),
      Focus: () => require('stores/tools/FocusStore'),
      GettingStarted: () => require('stores/gettingstarted/GettingStartedStore'),
      GlobalThroughput: () => require('stores/metrics/GlobalThroughputStore'),
      GrokPatterns: () => require('stores/grok-patterns/GrokPatternsStore'),
      IndexerCluster: () => require('stores/indexers/IndexerClusterStore'),
      IndexerFailures: () => require('stores/indexers/IndexerFailuresStore'),
      IndexRanges: () => require('stores/indices/IndexRangesStore'),
      Indices: () => require('stores/indices/IndicesStore'),
      Inputs: () => require('stores/inputs/InputsStore'),
      InputStates: () => require('stores/inputs/InputStatesStore'),
      InputStaticFields: () => require('stores/inputs/InputStaticFieldsStore'),
      InputTypes: () => require('stores/inputs/InputTypesStore'),
      Journal: () => require('stores/journal/JournalStore'),
      Loggers: () => require('stores/loggers/LoggersStore'),
      MessageCounts: () => require('stores/messages/MessageCountsStore'),
      MessageFields: () => require('stores/messages/MessageFieldsStore'),
      Messages: () => require('stores/messages/MessagesStore'),
      Metrics: () => require('stores/metrics/MetricsStore'),
      Nodes: () => require('stores/nodes/NodesStore'),
      Notifications: () => require('stores/notifications/NotificationsStore'),
      Outputs: () => require('stores/outputs/OutputsStore'),
      Plugins: () => require('stores/plugins/PluginsStore'),
      Preferences: () => require('stores/users/PreferencesStore'),
      Roles: () => require('stores/users/RolesStore'),
      SavedSearches: () => require('stores/search/SavedSearchesStore'),
      Search: () => require('stores/search/SearchStore'),
      Session: () => require('stores/sessions/SessionStore'),
      SingleNode: () => require('stores/nodes/SingleNodeStore'),
      Sources: () => require('stores/sources/SourcesStore'),
      Startpage: () => require('stores/users/StartpageStore'),
      StreamRules: () => require('stores/streams/StreamRulesStore'),
      Streams: () => require('stores/streams/StreamsStore'),
      System: () => require('stores/system/SystemStore'),
      SystemJobs: () => require('stores/systemjobs/SystemJobsStore'),
      SystemLoadBalancer: () => require('stores/load-balancer/SystemLoadBalancerStore'),
      SystemMessages: () => require('stores/systemmessages/SystemMessagesStore'),
      SystemProcessing: () => require('stores/system-processing/SystemProcessingStore'),
      SystemShutdown: () => require('stores/system-shutdown/SystemShutdownStore'),
      Tools: () => require('stores/tools/ToolsStore'),
      UniversalSearch: () => require('stores/search/UniversalSearchStore'),
      UsageStatsOptOut: () => require('stores/usagestats/UsageStatsOptOutStore'),
      Users: () => require('stores/users/UsersStore'),
      Widgets: () => require('stores/widgets/WidgetsStore'),
    };
    /* eslint-enable import/no-require */
  }

  getStore(storeName) {
    if (!this.stores[storeName]) {
      throw new Error('Requested store "' + storeName + '" is not registered.');
    }
    return this.stores[storeName]();
  }
}

export default StoreProvider;
