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

import type {
  IndicesConfigurationActionsType,
  IndicesConfigurationStoreState,
  RetentionStrategyResponse,
  RotationStrategyResponse,
} from 'components/indices/Types';
import UserNotification from 'util/UserNotification';
import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export const IndicesConfigurationActions = singletonActions(
  'core.IndicesConfiguration',
  () => Reflux.createActions<IndicesConfigurationActionsType>({
    loadRotationStrategies: { asyncResult: true },
    loadRetentionStrategies: { asyncResult: true },
  }),
);

const urlPrefix = '/system/indices';

export const IndicesConfigurationStore = singletonStore(
  'core.IndicesConfiguration',
  () => Reflux.createStore<IndicesConfigurationStoreState>({
    listenables: [IndicesConfigurationActions],

    rotationStrategies: undefined,
    retentionStrategies: undefined,
    retentionStrategiesContext: undefined,

    getInitialState() {
      return {
        activeRotationConfig: undefined,
        rotationStrategies: undefined,
        activeRetentionConfig: undefined,
        retentionStrategies: undefined,
        retentionStrategiesContext: undefined,
      };
    },
    getState() {
      return {
        activeRotationConfig: this.activeRotationConfig,
        rotationStrategies: this.rotationStrategies,
        activeRetentionConfig: this.activeRetentionConfig,
        retentionStrategies: this.retentionStrategies,
        retentionStrategiesContext: this.retentionStrategiesContext,
      };
    },
    _url(path) {
      return URLUtils.qualifyUrl(urlPrefix + path);
    },

    loadRotationStrategies() {
      const promise = fetch('GET', this._url('/rotation/strategies'));

      promise.then(
        (response: RotationStrategyResponse) => {
          this.rotationStrategies = response.strategies;
          this.trigger(this.getState());
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
        (response: RetentionStrategyResponse) => {
          this.retentionStrategiesContext = response.context;
          this.retentionStrategies = response.strategies;
          this.trigger(this.getState());
        },
        (error) => {
          UserNotification.error(`Fetching retention strategies failed: ${error}`, 'Could not retrieve retention strategies');
        },
      );

      IndicesConfigurationActions.loadRetentionStrategies.promise(promise);
    },
  }),
);
