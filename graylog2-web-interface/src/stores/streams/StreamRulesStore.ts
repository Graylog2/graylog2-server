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
import pull from 'lodash/pull';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { singletonStore } from 'logic/singleton';

type StreamRule = {
  field: string,
  type: number,
  value: string,
  inverted: boolean,
  description: string,
};

type Callback = {
  (): void,
};

// eslint-disable-next-line import/prefer-default-export
export const StreamRulesStore = singletonStore(
  'core.StreamRules',
  () => Reflux.createStore({
    callbacks: [],

    types() {
      const url = '/streams/null/rules/types';
      const promise = fetch('GET', URLUtils.qualifyUrl(url));

      return promise;
    },
    update(streamId: string, streamRuleId: string, data: StreamRule, callback: (() => void)) {
      const failCallback = (error) => {
        UserNotification.error(`Updating Stream Rule failed with status: ${error}`,
          'Could not update Stream Rule');
      };

      const url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.update(streamId, streamRuleId).url);
      const request = {
        field: data.field,
        type: data.type,
        value: data.value,
        inverted: data.inverted,
        description: data.description,
      };

      return fetch('PUT', url, request)
        .then(callback, failCallback)
        .then(this._emitChange.bind(this));
    },
    remove(streamId: string, streamRuleId: string, callback: (() => void)) {
      const failCallback = (error) => {
        UserNotification.error(`Deleting Stream Rule failed with status: ${error}`,
          'Could not delete Stream Rule');
      };

      const url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.delete(streamId, streamRuleId).url);

      fetch('DELETE', url)
        .then(callback, failCallback)
        .then(this._emitChange.bind(this));
    },
    create(streamId: string, data: StreamRule, callback: (() => void)) {
      const failCallback = (error) => {
        UserNotification.error(`Creating Stream Rule failed with status: ${error}`,
          'Could not create Stream Rule');
      };

      const url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.create(streamId).url);

      return fetch('POST', url, data)
        .then(callback, failCallback)
        .then(this._emitChange.bind(this));
    },
    onChange(callback: Callback) {
      this.callbacks.push(callback);
    },
    _emitChange() {
      this.callbacks.forEach((callback) => callback());
    },
    unregister(callback: Callback) {
      pull(this.callbacks, callback);
    },
  }),
);
