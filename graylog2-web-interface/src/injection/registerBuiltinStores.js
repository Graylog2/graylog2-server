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
};

const stores = {
  Authentication: () => require('stores/authentication/AuthenticationStore'),
  System: () => require('stores/system/SystemStore'),
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
