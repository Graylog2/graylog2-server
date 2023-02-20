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

import { singletonActions, singletonStore } from 'logic/singleton';
import type { SyncRefluxActions } from 'stores/StoreTypes';

type RefreshActionsType = {
  enable: () => void,
  disable: () => void,
  setInterval: (interval: number) => void,
  refresh: () => void,
};

export const RefreshActions: SyncRefluxActions<RefreshActionsType> = singletonActions(
  'views.Refresh',
  () => Reflux.createActions([
    'enable',
    'disable',
    'setInterval',
    'refresh',
  ] as const),
);

type RefreshConfig = {
  interval: number,
  enabled: boolean,
};

export const RefreshStore = singletonStore(
  'views.Refresh',
  () => Reflux.createStore<RefreshConfig>({
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

      this.intervalId = setInterval(RefreshActions.refresh, this.refreshConfig.interval);
      this._trigger();
    },

    enable() {
      this.refreshConfig = { ...this.refreshConfig, enabled: true };

      if (!this.intervalId) {
        this.intervalId = setInterval(RefreshActions.refresh, this.refreshConfig.interval);
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
