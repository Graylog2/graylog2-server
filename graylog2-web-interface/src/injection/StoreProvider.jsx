class StoreProvider {
  constructor() {
    /* eslint-disable import/no-require */
    this.stores = {
      AlarmCallbacks: () => require('stores/alarmcallbacks/AlarmCallbacksStore'),
      AlertConditions: () => require('stores/alertconditions/AlertConditionsStore'),
      ClusterOverview: () => require('stores/cluster/ClusterOverviewStore'),
      ConfigurationBundles: () => require('stores/configuration-bundles/ConfigurationBundlesStore'),
      Extractors: () => require('stores/extractors/ExtractorsStore'),
      GettingStarted: () => require('stores/gettingstarted/GettingStartedStore'),
      IndexerCluster: () => require('stores/indexers/IndexerClusterStore'),
      IndexerFailures: () => require('stores/indexers/IndexerFailuresStore'),
      Deflector: () => require('stores/indices/DeflectorStore'),
      IndexRanges: () => require('stores/indices/IndexRangesStore'),
      Indices: () => require('stores/indices/IndicesStore'),
      InputStates: () => require('stores/inputs/InputStatesStore'),
      InputStaticFields: () => require('stores/inputs/InputStaticFieldsStore'),
      InputTypes: () => require('stores/inputs/InputTypesStore'),
      Inputs: () => require('stores/inputs/InputsStore'),
      SystemLoadBalancer: () => require('stores/load-balancer/SystemLoadBalancerStore'),
      Loggers: () => require('stores/loggers/LoggersStore'),
      MessageCounts: () => require('stores/messages/MessageCountsStore'),
      MessageFields: () => require('stores/messages/MessageFieldsStore'),
      GlobalThroughput: () => require('stores/metrics/GlobalThroughputStore'),
      Metrics: () => require('stores/metrics/MetricsStore'),
      Nodes: () => require('stores/nodes/NodesStore'),
      SingleNode: () => require('stores/nodes/SingleNodeStore'),
      Notifications: () => require('stores/notifications/NotificationsStore'),
      Plugins: () => require('stores/plugins/PluginsStore'),
      SavedSearches: () => require('stores/search/SavedSearchesStore'),
      UniversalSearch: () => require('stores/search/UniversalSearchStore'),
      Session: () => require('stores/sessions/SessionStore'),
      SystemProcessing: () => require('stores/system-processing/SystemProcessingStore'),
      SystemShutdown: () => require('stores/system-shutdown/SystemShutdownStore'),
      System: () => require('stores/system/SystemStore'),
      SystemJobs: () => require('stores/systemjobs/SystemJobsStore'),
      SystemMessages: () => require('stores/systemmessages/SystemMessagesStore'),
      Focus: () => require('stores/tools/FocusStore'),
      CurrentUser: () => require('stores/users/CurrentUserStore'),
      Startpage: () => require('stores/users/StartpageStore'),
    };
    /* eslint-enable import/no-require */
  }

  getStore(storeName) {
    return this.stores[storeName]();
  }
}

export default StoreProvider;
