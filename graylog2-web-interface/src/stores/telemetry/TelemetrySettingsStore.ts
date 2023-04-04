import {singletonStore, singletonActions} from 'logic/singleton';
import Reflux from 'reflux';
import {RefluxActions} from 'stores/StoreTypes';
import {qualifyUrl} from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

export type UserTelemetrySettings = {
  telemetry_permission_asked: boolean;
  telemetry_enabled: boolean;
};

type TelemetrySettingsActionsType = RefluxActions<{
  update: (settings: Partial<UserTelemetrySettings>) => Promise<unknown>,
  get: () => Promise<unknown>,
}>;

export type TelemetrySettingsStoreState = {
  telemetrySettings: UserTelemetrySettings,
};

const urlPrefix = ApiRoutes.TelemetryApiController.setting().url;

export const TelemetrySettingsActions: TelemetrySettingsActionsType = singletonActions('telemetry.settings.actions', () => Reflux.createActions({
  update: {asyncResult: true},
  get: {asyncResult: true},
}));

export const TelemetrySettingsStore = singletonStore('telemetry.settings.store', () => Reflux.createStore<TelemetrySettingsStoreState>({
  listenables: [TelemetrySettingsActions],

  telemetrySetting: {},

  getInitialState() {
    return {
      telemetrySetting: this.telemetrySettings,
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
        UserNotification.error(`Update failed: ${error}`, `Could not update telemetry settings.`);
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
