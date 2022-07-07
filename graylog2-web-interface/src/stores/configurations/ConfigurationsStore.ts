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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { singletonStore, singletonActions } from 'logic/singleton';

type ConfigurationsActionsType = {
  list: (configType: any) => Promise<unknown>,
  listSearchesClusterConfig: () => Promise<unknown>,
  listMessageProcessorsConfig: (configType: any) => Promise<unknown>,
  listEventsClusterConfig: () => Promise<unknown>,
  listIndexDefaultsClusterConfig: () => Promise<unknown>,
  listWhiteListConfig: (configType: any) => Promise<unknown>,
  listPermissionsConfig: (configType: string) => Promise<unknown>,
  update: (configType: any, config: any) => Promise<void>,
  updateWhitelist: (configType: any, config: any) => Promise<void>,
  updateMessageProcessorsConfig: (configType: any, config: any) => Promise<void>,
}
export const ConfigurationsActions = singletonActions(
  'core.Configuration',
  () => Reflux.createActions<ConfigurationsActionsType>({
    list: { asyncResult: true },
    listSearchesClusterConfig: { asyncResult: true },
    listMessageProcessorsConfig: { asyncResult: true },
    listEventsClusterConfig: { asyncResult: true },
    listIndexDefaultsClusterConfig: { asyncResult: true },
    listWhiteListConfig: { asyncResult: true },
    listPermissionsConfig: { asyncResult: true },
    update: { asyncResult: true },
    updateWhitelist: { asyncResult: true },
    updateMessageProcessorsConfig: { asyncResult: true },
  }),
);

const urlPrefix = ApiRoutes.ClusterConfigResource.config().url;
export type Url = {
  id: string,
  value: string,
  title: string,
  type: string,
};

export type WhiteListConfig = {
  entries: Array<Url>,
  disabled: boolean,
};
export type PermissionsConfigType = {
  allow_sharing_with_everyone: boolean,
  allow_sharing_with_users: boolean,
}
export type ConfigurationsStoreState = {
  configuration: Record<string, any>,
  searchesClusterConfig: SearchesConfig,
  eventsClusterConfig: {},
  indexDefaultConfig: {},
};

export const ConfigurationsStore = singletonStore(
  'core.Configuration',
  () => Reflux.createStore<ConfigurationsStoreState>({
    listenables: [ConfigurationsActions],

    configuration: {},
    searchesClusterConfig: {},
    eventsClusterConfig: {},
    indexDefaultConfig: {},
    getInitialState() {
      return this.getState();
    },

    getState() {
      return {
        configuration: this.configuration,
        searchesClusterConfig: this.searchesClusterConfig,
        eventsClusterConfig: this.eventsClusterConfig,
        indexDefaultConfig: this.indexDefaultConfig,
      };
    },

    propagateChanges() {
      this.trigger(this.getState());
    },

    _url(path) {
      return qualifyUrl(urlPrefix + path);
    },

    list(configType) {
      const promise = fetch('GET', this._url(`/${configType}`));

      promise.then((response) => {
        this.configuration = { ...this.configuration, [configType]: response };
        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.list.promise(promise);
    },

    listSearchesClusterConfig() {
      const promise = fetch('GET', this._url('/org.graylog2.indexer.searches.SearchesClusterConfig')).then((response) => {
        this.searchesClusterConfig = response;
        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.listSearchesClusterConfig.promise(promise);
    },

    listMessageProcessorsConfig(configType) {
      const promise = fetch('GET', qualifyUrl('/system/messageprocessors/config')).then((response) => {
        this.configuration = { ...this.configuration, [configType]: response };
        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.listMessageProcessorsConfig.promise(promise);
    },

    listWhiteListConfig(configType) {
      const promise = fetch('GET', qualifyUrl('/system/urlwhitelist')).then((response) => {
        this.configuration = { ...this.configuration, [configType]: response };
        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.listWhiteListConfig.promise(promise);
    },

    listPermissionsConfig(configType) {
      const promise = fetch('GET', this._url(`/${configType}`)).then((response: PermissionsConfigType) => {
        this.configuration = {
          ...this.configuration,
          // default values bellow should be the same in backend.
          [configType]: response || {
            allow_sharing_with_everyone: true,
            allow_sharing_with_users: true,
          },
        };

        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.listPermissionsConfig.promise(promise);
    },

    listEventsClusterConfig() {
      const promise = fetch('GET', this._url('/org.graylog.events.configuration.EventsConfiguration')).then((response) => {
        this.eventsClusterConfig = response;
        this.propagateChanges();

        return response;
      });

      ConfigurationsActions.listEventsClusterConfig.promise(promise);
    },

    listIndexDefaultsClusterConfig() {
      const promise = fetch('GET', this._url('/org.graylog2.configuration.IndexDefaultsConfiguration')).then((response) => {
        this.indexDefaultConfig = response;
        this.propagateChanges();

        return response;
      });
      ConfigurationsActions.listIndexDefaultsClusterConfig.promise(promise);
    },

    update(configType, config) {
      const promise = fetch('PUT', this._url(`/${configType}`), config);

      promise.then(
        (response) => {
          this.configuration = { ...this.configuration, [configType]: response };
          this.propagateChanges();
          UserNotification.success('Configuration updated successfully');

          return response;
        },
        (error) => {
          UserNotification.error(`Search config update failed: ${error}`, `Could not update search config: ${configType}`);
        },
      );

      ConfigurationsActions.update.promise(promise);
    },

    updateWhitelist(configType, config) {
      const promise = fetch('PUT', qualifyUrl('/system/urlwhitelist'), config);

      promise.then(
        () => {
          this.configuration = { ...this.configuration, [configType]: config };
          this.propagateChanges();
          UserNotification.success('Url Whitelist Configuration updated successfully');

          return config;
        },
        (error) => {
          UserNotification.error(`Url Whitelist config update failed: ${error}`, `Could not update Url Whitelist: ${configType}`);
        },
      );

      ConfigurationsActions.updateWhitelist.promise(promise);
    },

    updateMessageProcessorsConfig(configType, config) {
      const promise = fetch('PUT', qualifyUrl('/system/messageprocessors/config'), config);

      promise.then(
        (response) => {
          this.configuration = { ...this.configuration, [configType]: response };
          this.propagateChanges();
          UserNotification.success('Configuration updated successfully');

          return response;
        },
        (error) => {
          UserNotification.error(`Message processors config update failed: ${error}`, `Could not update config: ${configType}`);
        },
      );

      ConfigurationsActions.updateMessageProcessorsConfig.promise(promise);
    },
  }),
);
