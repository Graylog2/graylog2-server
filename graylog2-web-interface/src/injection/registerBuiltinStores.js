/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import CombinedProvider from './CombinedProvider';

/* eslint-disable global-require */
const actions = {
  Authentication: () => require('actions/authentication/AuthenticationActions'),
  Events: () => require('actions/events/EventsActions'),
  Extractors: () => require('actions/extractors/ExtractorsActions'),
  GettingStarted: () => require('actions/gettingstarted/GettingStartedActions'),
  IndexerCluster: () => require('actions/indexers/IndexerClusterActions'),
  IndexerOverview: () => require('actions/indexers/IndexerOverviewActions'),
  IndexRanges: () => require('actions/indices/IndexRangesActions'),
  IndexSets: () => require('actions/indices/IndexSetsActions'),
  Indices: () => require('actions/indices/IndicesActions'),
  IndicesConfiguration: () => require('actions/indices/IndicesConfigurationActions'),
  Inputs: () => require('actions/inputs/InputsActions'),
  InputTypes: () => require('actions/inputs/InputTypesActions'),
  Loggers: () => require('actions/system/LoggersActions'),
  LookupTableCaches: () => require('actions/lookup-tables/LookupTableCachesActions'),
  LookupTableDataAdapters: () => require('actions/lookup-tables/LookupTableDataAdaptersActions'),
  LookupTables: () => require('actions/lookup-tables/LookupTablesActions'),
  Messages: () => require('actions/messages/MessagesActions'),
  Metrics: () => require('actions/metrics/MetricsActions'),
  Nodes: () => require('actions/nodes/NodesActions'),
  Notifications: () => require('actions/notifications/NotificationsActions'),
  PipelineConnections: () => require('actions/pipelines/PipelineConnectionsActions'),
  Pipelines: () => require('actions/pipelines/PipelinesActions'),
  Preferences: () => require('actions/users/PreferencesActions'),
  FilterPreview: () => require('actions/event-definitions/FilterPreviewActions'),
  Rules: () => require('actions/rules/RulesActions'),
  ServerAvailability: () => require('actions/sessions/ServerAvailabilityActions'),
  Session: () => require('actions/sessions/SessionActions'),
  Sidecars: () => require('actions/sidecars/SidecarsActions'),
  SidecarsAdministration: () => require('actions/sidecars/SidecarsAdministrationActions'),
  Simulator: () => require('actions/simulator/SimulatorActions'),
  SingleNode: () => require('actions/nodes/SingleNodeActions'),
  SystemJobs: () => require('actions/systemjobs/SystemJobsActions'),
};

const stores = {
  Authentication: () => require('stores/authentication/AuthenticationStore'),
  Events: () => require('stores/events/EventsStore'),
  Extractors: () => require('stores/extractors/ExtractorsStore'),
  GettingStarted: () => require('stores/gettingstarted/GettingStartedStore'),
  GlobalThroughput: () => require('stores/metrics/GlobalThroughputStore'),
  GrokPatterns: () => require('stores/grok-patterns/GrokPatternsStore'),
  IndexerCluster: () => require('stores/indexers/IndexerClusterStore'),
  IndexerFailures: () => require('stores/indexers/IndexerFailuresStore'),
  IndexerOverview: () => require('stores/indexers/IndexerOverviewStore'),
  IndexRanges: () => require('stores/indices/IndexRangesStore'),
  IndexSets: () => require('stores/indices/IndexSetsStore'),
  Indices: () => require('stores/indices/IndicesStore'),
  IndicesConfiguration: () => require('stores/indices/IndicesConfigurationStore'),
  Inputs: () => require('stores/inputs/InputsStore'),
  InputStates: () => require('stores/inputs/InputStatesStore'),
  InputStaticFields: () => require('stores/inputs/InputStaticFieldsStore'),
  InputTypes: () => require('stores/inputs/InputTypesStore'),
  Journal: () => require('stores/journal/JournalStore'),
  Loggers: () => require('stores/system/LoggersStore'),
  LookupTables: () => require('stores/lookup-tables/LookupTablesStore'),
  LookupTableCaches: () => require('stores/lookup-tables/LookupTableCachesStore'),
  LookupTableDataAdapters: () => require('stores/lookup-tables/LookupTableDataAdaptersStore'),
  Messages: () => require('stores/messages/MessagesStore'),
  Metrics: () => require('stores/metrics/MetricsStore'),
  Nodes: () => require('stores/nodes/NodesStore'),
  Notifications: () => require('stores/notifications/NotificationsStore'),
  Outputs: () => require('stores/outputs/OutputsStore'),
  PipelineConnections: () => require('stores/pipelines/PipelineConnectionsStore'),
  Pipelines: () => require('stores/pipelines/PipelinesStore'),
  Plugins: () => require('stores/plugins/PluginsStore'),
  Preferences: () => require('stores/users/PreferencesStore'),
  FilterPreview: () => require('stores/event-definitions/FilterPreviewStore'),
  Roles: () => require('stores/users/RolesStore'),
  Rules: () => require('stores/rules/RulesStore'),
  ServerAvailability: () => require('stores/sessions/ServerAvailabilityStore'),
  Session: () => require('stores/sessions/SessionStore'),
  Sidecars: () => require('stores/sidecars/SidecarsStore'),
  SidecarsAdministration: () => require('stores/sidecars/SidecarsAdministrationStore'),
  Simulator: () => require('stores/simulator/SimulatorStore'),
  SingleNode: () => require('stores/nodes/SingleNodeStore'),
  Startpage: () => require('stores/users/StartpageStore'),
  StreamRules: () => require('stores/streams/StreamRulesStore'),
  System: () => require('stores/system/SystemStore'),
  SystemJobs: () => require('stores/systemjobs/SystemJobsStore'),
  SystemLoadBalancer: () => require('stores/load-balancer/SystemLoadBalancerStore'),
  SystemMessages: () => require('stores/systemmessages/SystemMessagesStore'),
  SystemProcessing: () => require('stores/system-processing/SystemProcessingStore'),
  ConfigurationVariable: () => require('stores/sidecars/ConfigurationVariableStore'),
  UniversalSearch: () => require('stores/search/UniversalSearchStore'),
  Users: () => require('stores/users/UsersStore'),
};
/* eslint-enable global-require */

export default () => {
  Object.keys(actions).forEach((key) => CombinedProvider.registerAction(key, actions[key]));
  Object.keys(stores).forEach((key) => CombinedProvider.registerStore(key, stores[key]));
};
