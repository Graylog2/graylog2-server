/* global actionsProvider */

class ActionsProvider {
  constructor() {
    /* eslint-disable import/no-require */
    this.actions = {
      AlarmCallbacks: () => require('actions/alarmcallbacks/AlarmCallbacksActions'),
      AlertConditions: () => require('actions/alertconditions/AlertConditionsActions'),
      AlertReceivers: () => require('actions/alertreceivers/AlertReceiversActions'),
      Alerts: () => require('actions/alerts/AlertsActions'),
      Configuration: () => require('actions/configurations/ConfigurationActions'),
      ConfigurationBundles: () => require('actions/configuration-bundles/ConfigurationBundlesActions'),
      Deflector: () => require('actions/indices/DeflectorActions'),
      Extractors: () => require('actions/extractors/ExtractorsActions'),
      GettingStarted: () => require('actions/gettingstarted/GettingStartedActions'),
      HistogramData: () => require('actions/sources/HistogramDataActions'),
      IndexerCluster: () => require('actions/indexers/IndexerClusterActions'),
      IndexerOverview: () => require('actions/indexers/IndexerOverviewActions'),
      IndexRanges: () => require('actions/indices/IndexRangesActions'),
      Indices: () => require('actions/indices/IndicesActions'),
      IndicesConfiguration: () => require('actions/indices/IndicesConfigurationActions'),
      Inputs: () => require('actions/inputs/InputsActions'),
      InputTypes: () => require('actions/inputs/InputTypesActions'),
      Ldap: () => require('actions/ldap/LdapActions'),
      LdapGroups: () => require('actions/ldap/LdapGroupsActions'),
      Loggers: () => require('actions/system/LoggersActions'),
      MessageCounts: () => require('actions/messages/MessageCountsActions'),
      Messages: () => require('actions/messages/MessagesActions'),
      Metrics: () => require('actions/metrics/MetricsActions'),
      Nodes: () => require('actions/nodes/NodesActions'),
      Notifications: () => require('actions/notifications/NotificationsActions'),
      Refresh: () => require('actions/tools/RefreshActions'),
      SavedSearches: () => require('actions/search/SavedSearchesActions'),
      ServerAvailability: () => require('actions/sessions/ServerAvailabilityActions'),
      Session: () => require('actions/sessions/SessionActions'),
      SingleNode: () => require('actions/nodes/SingleNodeActions'),
      Streams: () => require('actions/streams/StreamsActions'),
      SystemJobs: () => require('actions/systemjobs/SystemJobsActions'),
      Widgets: () => require('actions/widgets/WidgetsActions'),
    };
    /* eslint-enable import/no-require */
  }

  getActions(actionsName) {
    if (!this.actions[actionsName]) {
      throw new Error(`Requested actions "${actionsName}" is not registered.`);
    }
    return this.actions[actionsName]();
  }
}

if (typeof actionsProvider === 'undefined') {
  window.actionsProvider = new ActionsProvider();
}

export default actionsProvider;
