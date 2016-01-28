class ActionsProvider {
  constructor() {
    this.actions = {
      AlarmCallbacks: () => require('actions/alarmcallbacks/AlarmCallbacksActions'),
      AlertConditions: () => require('actions/alertconditions/AlertConditionsActions'),
      AlertReceivers: () => require('actions/alertreceivers/AlertReceiversActions'),
      ConfigurationBundles: () => require('actions/configuration-bundles/ConfigurationBundlesActions'),
      Extractors: () => require('actions/extractors/ExtractorsActions'),
      GettingStarted: () => require('actions/gettingstarted/GettingStartedActions'),
      IndexerCluster: () => require('actions/indexers/IndexerClusterActions'),
      Deflector: () => require('actions/indices/DeflectorActions'),
      IndexRanges: () => require('actions/indices/IndexRangesActions'),
      Indices: () => require('actions/indices/IndicesActions'),
      InputTypes: () => require('actions/inputs/InputTypesActions'),
      Inputs: () => require('actions/inputs/InputsActions'),
      MessageCounts: () => require('actions/messages/MessageCountsActions'),
      Metrics: () => require('actions/metrics/MetricsActions'),
      Nodes: () => require('actions/nodes/NodesActions'),
      SingleNode: () => require('actions/nodes/SingleNodeActions'),
      Notifications: () => require('actions/notifications/NotificationsActions'),
      SavedSearches: () => require('actions/search/SavedSearchesActions'),
      Session: () => require('actions/sessions/SessionActions'),
      Streams: () => require('actions/streams/StreamsActions'),
      SystemJobs: () => require('actions/systemjobs/SystemJobsActions'),
      Widgets: () => require('actions/widgets/WidgetsActions'),
    };
  }

  getActions(actionsName) {
    return this.actions[actionsName];
  }
}

export default ActionsProvider;
