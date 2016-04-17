import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
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

  _addConfigType(strategies, data) {
    // The config object needs to have the "type" field set to the "default_config.type" to make the REST call work.
    const result = strategies.filter((strategy) => strategy.type === data.strategy)[0];

    if (result) {
      data.config.type = result.default_config.type;
    }
  },

  loadRotationConfig() {
    const promise = fetch('GET', this._url('/rotation/config'));

    promise.then(
      response => {
        this.trigger({activeRotationConfig: response});
      },
      error => {
        UserNotification.error(`Fetching rotation config failed: ${error}`, 'Could not retrieve rotation config');
      });

    IndicesConfigurationActions.loadRotationConfig.promise(promise);
  },

  loadRotationStrategies() {
    const promise = fetch('GET', this._url('/rotation/strategies'));

    promise.then(
      response => {
        this.rotationStrategies = response.strategies;
        this.trigger({rotationStrategies: response.strategies});
      },
      error => {
        UserNotification.error(`Fetching rotation strategies failed: ${error}`, 'Could not retrieve rotation strategies');
      });

    IndicesConfigurationActions.loadRotationStrategies.promise(promise);
  },

  loadRetentionConfig() {
    const promise = fetch('GET', this._url('/retention/config'));

    promise.then(
      response => {
        this.trigger({activeRetentionConfig: response});
      },
      error => {
        UserNotification.error(`Fetching retention config failed: ${error}`, 'Could not retrieve retention config');
      });

    IndicesConfigurationActions.loadRetentionConfig.promise(promise);
  },

  loadRetentionStrategies() {
    const promise = fetch('GET', this._url('/retention/strategies'));

    promise.then(
      response => {
        this.retentionStrategies = response.strategies;
        this.trigger({retentionStrategies: response.strategies});
      },
      error => {
        UserNotification.error(`Fetching retention strategies failed: ${error}`, 'Could not retrieve retention strategies');
      });

    IndicesConfigurationActions.loadRetentionStrategies.promise(promise);
  },

  updateRotationConfiguration(data) {
    this._addConfigType(this.rotationStrategies, data);

    const promise = fetch('PUT', this._url('/rotation/config'), data);

    promise.then(
      response => {
        this.trigger({activeRotationConfig: response});
        UserNotification.success('Index rotation configuration updated successfully');
      },
      error => {
        UserNotification.error(`Updating index rotation config failed: ${error}`, 'Unable to update index rotation config');
      });

    IndicesConfigurationActions.updateRotationConfiguration.promise(promise);
  },

  updateRetentionConfiguration(data) {
    this._addConfigType(this.retentionStrategies, data);

    const promise = fetch('PUT', this._url('/retention/config'), data);

    promise.then(
      response => {
        this.trigger({activeRetentionConfig: response});
        UserNotification.success('Index retention configuration updated successfully');
      },
      error => {
        UserNotification.error(`Updating index retention config failed: ${error}`, 'Unable to update index retention config');
      });

    IndicesConfigurationActions.updateRetentionConfiguration.promise(promise);
  },
});

export default IndicesConfigurationStore;
