import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';
const ConfigurationActions = ActionsProvider.getActions('Configuration');

const urlPrefix = '/system/cluster_config';

const ConfigurationsStore = Reflux.createStore({
  listenables: [ConfigurationActions],

  configuration: {},

  _url(path) {
    return URLUtils.qualifyUrl(urlPrefix + path);
  },

  list(configType) {
    const promise = fetch('GET', this._url(`/${configType}`));
    promise.then((response) => {
      this.configuration[configType] = response;
      this.trigger({ configuration: this.configuration });
      return response;
    });

    ConfigurationActions.list.promise(promise);
  },

  listSearchesClusterConfig() {
    const promise = fetch('GET', this._url('/org.graylog2.indexer.searches.SearchesClusterConfig')).then((response) => {
      this.trigger({ searchesClusterConfig: response });
      return response;
    });

    ConfigurationActions.listSearchesClusterConfig.promise(promise);
  },

  listMessageProcessorsConfig(configType) {
    const promise = fetch('GET', URLUtils.qualifyUrl('/system/messageprocessors/config')).then((response) => {
      this.configuration[configType] = response;
      this.trigger({ configuration: this.configuration });
      return response;
    });

    ConfigurationActions.listMessageProcessorsConfig.promise(promise);
  },

  update(configType, config) {
    const promise = fetch('PUT', this._url(`/${configType}`), config);

    promise.then(
      (response) => {
        this.configuration[configType] = response;
        this.trigger({ configuration: this.configuration });
        UserNotification.success('Configuration updated successfully');
        return response;
      },
      (error) => {
        UserNotification.error(`Search config update failed: ${error}`, `Could not update search config: ${configType}`);
      });

    ConfigurationActions.update.promise(promise);
  },

  updateMessageProcessorsConfig(configType, config) {
    const promise = fetch('PUT', URLUtils.qualifyUrl('/system/messageprocessors/config'), config);

    promise.then(
      (response) => {
        this.configuration[configType] = response;
        this.trigger({ configuration: this.configuration });
        UserNotification.success('Configuration updated successfully');
        return response;
      },
      (error) => {
        UserNotification.error(`Message processors config update failed: ${error}`, `Could not update config: ${configType}`);
      });

    ConfigurationActions.updateMessageProcessorsConfig.promise(promise);
  },
});

export default ConfigurationsStore;
