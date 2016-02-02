class StoreProvider {
  constructor() {
    /* eslint-disable import/no-require */
    this.stores = {
      AlarmCallbacks: () => require('stores/alarmcallbacks/AlarmCallbacksStore'),
      AlertConditions: () => require('stores/alertconditions/AlertConditionsStore'),
      ClusterOverview: () => require('stores/cluster/ClusterOverviewStore'),
      ConfigurationBundles: () => require('stores/configuration-bundles/ConfigurationBundlesStore'),
      CurrentUser: () => require('stores/users/CurrentUserStore'),
      Deflector: () => require('stores/indices/DeflectorStore'),
      Extractors: () => require('stores/extractors/ExtractorsStore'),
      Focus: () => require('stores/tools/FocusStore'),
      GettingStarted: () => require('stores/gettingstarted/GettingStartedStore'),
      GlobalThroughput: () => require('stores/metrics/GlobalThroughputStore'),
      IndexRanges: () => require('stores/indices/IndexRangesStore'),
      IndexerCluster: () => require('stores/indexers/IndexerClusterStore'),
      IndexerFailures: () => require('stores/indexers/IndexerFailuresStore'),
      Indices: () => require('stores/indices/IndicesStore'),
      InputStates: () => require('stores/inputs/InputStatesStore'),
      InputStaticFields: () => require('stores/inputs/InputStaticFieldsStore'),
      InputTypes: () => require('stores/inputs/InputTypesStore'),
      Inputs: () => require('stores/inputs/InputsStore'),
      Journal: () => require('stores/journal/JournalStore'),
      Loggers: () => require('stores/loggers/LoggersStore'),
      MessageCounts: () => require('stores/messages/MessageCountsStore'),
      MessageFields: () => require('stores/messages/MessageFieldsStore'),
      Metrics: () => require('stores/metrics/MetricsStore'),
      Nodes: () => require('stores/nodes/NodesStore'),
      Notifications: () => require('stores/notifications/NotificationsStore'),
      Plugins: () => require('stores/plugins/PluginsStore'),
      SavedSearches: () => require('stores/search/SavedSearchesStore'),
      Session: () => require('stores/sessions/SessionStore'),
      SingleNode: () => require('stores/nodes/SingleNodeStore'),
      Startpage: () => require('stores/users/StartpageStore'),
      System: () => require('stores/system/SystemStore'),
      SystemJobs: () => require('stores/systemjobs/SystemJobsStore'),
      SystemLoadBalancer: () => require('stores/load-balancer/SystemLoadBalancerStore'),
      SystemMessages: () => require('stores/systemmessages/SystemMessagesStore'),
      SystemProcessing: () => require('stores/system-processing/SystemProcessingStore'),
      SystemShutdown: () => require('stores/system-shutdown/SystemShutdownStore'),
      UniversalSearch: () => require('stores/search/UniversalSearchStore'),
    };
    /* eslint-enable import/no-require */
  }

  getStore(storeName) {
    return this.stores[storeName]();
  }
}

export default StoreProvider;
