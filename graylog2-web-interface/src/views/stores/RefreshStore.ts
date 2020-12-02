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

import SearchActions from 'views/actions/SearchActions';
import { singletonActions, singletonStore } from 'views/logic/singleton';

type RefreshActionsType = {
  enable: () => void,
  disable: () => void,
  setInterval: (number) => void,
};

export const RefreshActions: RefreshActionsType = singletonActions(
  'views.Refresh',
  () => Reflux.createActions([
    'enable',
    'disable',
    'setInterval',
  ]),
);

export const RefreshStore = singletonStore(
  'views.Refresh',
  () => Reflux.createStore({
    listenables: [RefreshActions],

    refreshConfig: {},

    intervalId: undefined,

    init() {
      this.refreshConfig = {
        enabled: false,
        interval: 1000,
      };
    },

    getInitialState() {
      return this.refreshConfig;
    },

    setInterval(interval: number) {
      this.refreshConfig = { interval, enabled: true };

      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = undefined;
      }

      this.intervalId = setInterval(SearchActions.executeWithCurrentState, this.refreshConfig.interval);
      this._trigger();
    },

    enable() {
      this.refreshConfig = { ...this.refreshConfig, enabled: true };

      if (!this.intervalId) {
        this.intervalId = setInterval(SearchActions.executeWithCurrentState, this.refreshConfig.interval);
      }

      this._trigger();
    },

    disable() {
      this.refreshConfig = { ...this.refreshConfig, enabled: false };

      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = undefined;
      }

      this._trigger();
    },

    _trigger() {
      const { enabled, interval } = this.refreshConfig;

      this.trigger({ enabled, interval });
    },
  }),
);
