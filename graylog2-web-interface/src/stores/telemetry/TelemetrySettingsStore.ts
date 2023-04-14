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

import { singletonStore, singletonActions } from 'logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

export type UserTelemetrySettings = {
  telemetry_permission_asked: boolean;
  telemetry_enabled: boolean;
};

type TelemetrySettingsActionsType = RefluxActions<{
  update: (settings: Partial<UserTelemetrySettings>) => Promise<unknown>,
  get: () => Promise<UserTelemetrySettings>,
}>;

export type TelemetrySettingsStoreState = {
  telemetrySetting: UserTelemetrySettings,
};

const urlPrefix = ApiRoutes.TelemetryApiController.setting().url;

export const TelemetrySettingsActions: TelemetrySettingsActionsType = singletonActions('telemetry.settings.actions', () => Reflux.createActions({
  update: { asyncResult: true },
  get: { asyncResult: true },
}));
export const TelemetrySettingsStore = singletonStore('telemetry.settings.store', () => Reflux.createStore<TelemetrySettingsStoreState>({
  listenables: [TelemetrySettingsActions],

  telemetrySetting: undefined,

  getInitialState() {
    return {
      telemetrySetting: this.telemetrySetting,
    };
  },

  init() {
    this.get();
  },

  get() {
    const promise = fetch('GET', this._url());

    promise.then((response) => {
      this.telemetrySetting = response;
      this.propagateChanges();

      return response;
    });

    TelemetrySettingsActions.get.promise(promise);
  },

  update(settings: UserTelemetrySettings) {
    const promise = fetch('PUT', this._url(), settings);

    promise.then(
      (response) => {
        this.telemetrySetting = response;
        this.propagateChanges();

        return response;
      },
      (error) => {
        UserNotification.error(`Update failed: ${error}`, 'Could not update telemetry settings.');
      },
    );

    TelemetrySettingsActions.update.promise(promise);
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      telemetrySetting: this.telemetrySetting,
    };
  },

  _url(): string {
    return qualifyUrl(urlPrefix);
  },

}));
