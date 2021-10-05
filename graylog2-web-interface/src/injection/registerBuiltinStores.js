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
