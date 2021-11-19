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
import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const IndicesConfigurationActions = ActionsProvider.getActions('IndicesConfiguration');

const urlPrefix = '/system/indices';

const IndicesConfigurationStore = Reflux.createStore({
  listenables: [IndicesConfigurationActions],

  rotationStrategies: undefined,
  retentionStrategies: undefined,

  getInitialState() {
    return {
      activeRotationConfig: undefined,
      rotationStrategies: undefined,
      activeRetentionConfig: undefined,
      retentionStrategies: undefined,
    };
  },

  _url(path) {
    return URLUtils.qualifyUrl(urlPrefix + path);
  },

  loadRotationStrategies() {
    const promise = fetch('GET', this._url('/rotation/strategies'));

    promise.then(
      (response) => {
        this.rotationStrategies = response.strategies;
        this.trigger({ rotationStrategies: response.strategies });
      },
      (error) => {
        UserNotification.error(`Fetching rotation strategies failed: ${error}`, 'Could not retrieve rotation strategies');
      },
    );

    IndicesConfigurationActions.loadRotationStrategies.promise(promise);
  },

  loadRetentionStrategies() {
    const promise = fetch('GET', this._url('/retention/strategies'));

    promise.then(
      (response) => {
        this.retentionStrategies = response.strategies;
        this.trigger({ retentionStrategies: response.strategies });
      },
      (error) => {
        UserNotification.error(`Fetching retention strategies failed: ${error}`, 'Could not retrieve retention strategies');
      },
    );

    IndicesConfigurationActions.loadRetentionStrategies.promise(promise);
  },
});

export default IndicesConfigurationStore;
