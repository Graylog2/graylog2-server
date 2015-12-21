import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const InputStatesStore = Reflux.createStore({
  listenables: [],

  init() {
    this.list();
  },

  getInitialState() {
    return {inputStates: this.inputStates};
  },

  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterInputStatesController.list().url);
    return fetch('GET', url)
      .then((response) => {
        const result = {};
        Object.keys(response).forEach((node) => {
          response[node].forEach((input) => {
            if (!result[input.id]) {
              result[input.id] = {};
            }
            result[input.id][node] = input;
          });
        });
        this.inputStates = result;
        this.trigger({inputStates: this.inputStates});

        return result;
      });
  },

  start(input) {
    let url;

    if (input.global) {
      url = URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterInputStatesController.start(input.id).url);
    } else {
      url = URLUtils.qualifyUrl(jsRoutes.controllers.api.InputStatesController.start(input.id).url);
    }

    return fetch('PUT', url)
      .then(
        () => {
          this.list();
          UserNotification.success(`Input '${input.title}' started successfully`);
        },
        error => {
          UserNotification.error(`Error starting input '${input.title}': ${error}`, `Input '${input.title}' could not be started`);
        });
  },

  stop(input) {
    let url;

    if (input.global) {
      url = URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterInputStatesController.stop(input.id).url);
    } else {
      url = URLUtils.qualifyUrl(jsRoutes.controllers.api.InputStatesController.stop(input.id).url);
    }

    return fetch('DELETE', url)
      .then(
        () => {
          this.list();
          UserNotification.success(`Input '${input.title}' stopped successfully`);
        },
        error => {
          UserNotification.error(`Error stopping input '${input.title}': ${error}`, `Input '${input.title}' could not be stopped`);
        });
  },
});

export default InputStatesStore;
