import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import ConfigurationActions from 'actions/configurations/ConfigurationActions';

const urlPrefix = '/system/cluster_config';

const ConfigurationsStore = Reflux.createStore({
  listenables: [ConfigurationActions],

  _url(path) {
    return URLUtils.qualifyUrl(urlPrefix + path);
  },

  list(configType) {
    const promise = fetch('GET', this._url(`/${configType}`)).then((response) => {
      const configuration = {};
      configuration[configType] = response;
      this.trigger({configuration: configuration});
    });

    ConfigurationActions.list.promise(promise);
  },

  update(configType, config) {
    const promise = fetch('PUT', this._url(`/${configType}`), config);

    promise.then((response) => {
      const configuration = {};
      configuration[configType] = response;
      this.trigger({configuration: configuration});
    }, this._errorHandler('Search config update failed', `Could not update search config: ${configType}`));

    ConfigurationActions.update.promise(promise);
  },

  _errorHandler(message, title) {
    return (error) => {
      let errorMessage;
      try {
        errorMessage = error.additional.body.message;
      } catch (e) {
        errorMessage = error.message;
      }
      UserNotification.error(`${message}: ${errorMessage}`, title);
    };
  },
});

export default ConfigurationsStore;
