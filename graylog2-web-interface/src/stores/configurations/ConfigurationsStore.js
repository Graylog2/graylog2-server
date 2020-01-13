// @flow
import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';

const ConfigurationActions = ActionsProvider.getActions('Configuration');

const urlPrefix = '/system/cluster_config';
export type Url = {
  id: string,
  value: string,
  title: string,
  type: string,
};

export type WhiteListConfig = {
  entries: Array<Url>,
  disabled: boolean
};
const ConfigurationsStore = Reflux.createStore({
  listenables: [ConfigurationActions],

  configuration: {},
  searchesClusterConfig: {},
  eventsClusterConfig: {},
  getInitialState() {
    return this.getState();
  },

  getState() {
    return {
      configuration: this.configuration,
      searchesClusterConfig: this.searchesClusterConfig,
      eventsClusterConfig: this.eventsClusterConfig,
    };
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  _url(path) {
    return URLUtils.qualifyUrl(urlPrefix + path);
  },

  list(configType) {
    const promise = fetch('GET', this._url(`/${configType}`));
    promise.then((response) => {
      this.configuration[configType] = response;
      this.propagateChanges();
      return response;
    });

    ConfigurationActions.list.promise(promise);
  },

  listSearchesClusterConfig() {
    const promise = fetch('GET', this._url('/org.graylog2.indexer.searches.SearchesClusterConfig')).then((response) => {
      this.searchesClusterConfig = response;
      this.propagateChanges();
      return response;
    });

    ConfigurationActions.listSearchesClusterConfig.promise(promise);
  },

  listMessageProcessorsConfig(configType) {
    const promise = fetch('GET', URLUtils.qualifyUrl('/system/messageprocessors/config')).then((response) => {
      this.configuration[configType] = response;
      this.propagateChanges();
      return response;
    });

    ConfigurationActions.listMessageProcessorsConfig.promise(promise);
  },

  listWhiteListConfig(configType) {
    const promise = fetch('GET', URLUtils.qualifyUrl('/system/urlwhitelist')).then((response) => {
      this.configuration[configType] = response;
      this.propagateChanges();
      return response;
    });

    ConfigurationActions.listMessageProcessorsConfig.promise(promise);
  },

  listEventsClusterConfig() {
    const promise = fetch('GET', this._url('/org.graylog.events.configuration.EventsConfiguration')).then((response) => {
      this.eventsClusterConfig = response;
      this.propagateChanges();
      return response;
    });

    ConfigurationActions.listEventsClusterConfig.promise(promise);
  },

  update(configType, config) {
    const promise = fetch('PUT', this._url(`/${configType}`), config);

    promise.then(
      (response) => {
        this.configuration[configType] = response;
        this.propagateChanges();
        UserNotification.success('Configuration updated successfully');
        return response;
      },
      (error) => {
        UserNotification.error(`Search config update failed: ${error}`, `Could not update search config: ${configType}`);
      },
    );

    ConfigurationActions.update.promise(promise);
  },

  updateWhitelist(configType, config) {
    const promise = fetch('PUT', URLUtils.qualifyUrl('/system/urlwhitelist'), config);

    promise.then(
      () => {
        this.configuration[configType] = config;
        this.propagateChanges();
        UserNotification.success('Url Whitelist Configuration updated successfully');
        return config;
      },
      (error) => {
        UserNotification.error(`Url Whitelist config update failed: ${error}`, `Could not update Url Whitelist: ${configType}`);
      },
    );

    ConfigurationActions.updateWhitelist.promise(promise);
  },

  updateMessageProcessorsConfig(configType, config) {
    const promise = fetch('PUT', URLUtils.qualifyUrl('/system/messageprocessors/config'), config);

    promise.then(
      (response) => {
        this.configuration[configType] = response;
        this.propagateChanges();
        UserNotification.success('Configuration updated successfully');
        return response;
      },
      (error) => {
        UserNotification.error(`Message processors config update failed: ${error}`, `Could not update config: ${configType}`);
      },
    );

    ConfigurationActions.updateMessageProcessorsConfig.promise(promise);
  },
});

export default ConfigurationsStore;
