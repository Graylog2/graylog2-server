import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const InputStatesStore = Reflux.createStore({
  listenables: [],

  init() {
    this.list();
  },

  getInitialState() {
    return { inputStates: this.inputStates };
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterInputStatesController.list().url);
    return fetch('GET', url)
      .then((response) => {
        const result = {};
        Object.keys(response).forEach((node) => {
          if (!response[node]) {
            return;
          }
          response[node].forEach((input) => {
            if (!result[input.id]) {
              result[input.id] = {};
            }
            result[input.id][node] = input;
          });
        });
        this.inputStates = result;
        this.trigger({ inputStates: this.inputStates });

        return result;
      });
  },

  _checkInputStateChangeResponse(input, response, action) {
    const nodes = Object.keys(response).filter((node) => (input.global ? true : node === input.node));
    const failedNodes = nodes.filter((nodeId) => response[nodeId] === null);

    if (failedNodes.length === 0) {
      UserNotification.success(`Request to ${action.toLowerCase()} input '${input.title}' was sent successfully.`,
        `Input '${input.title}' will be ${action === 'START' ? 'started' : 'stopped'} shortly`);
    } else if (failedNodes.length === nodes.length) {
      UserNotification.error(`Request to ${action.toLowerCase()} input '${input.title}' failed. Check your Graylog logs for more information.`,
        `Input '${input.title}' could not be ${action === 'START' ? 'started' : 'stopped'}`);
    } else {
      UserNotification.warning(`Request to ${action.toLowerCase()} input '${input.title}' failed in some nodes. Check your Graylog logs for more information.`,
        `Input '${input.title}' could not be ${action === 'START' ? 'started' : 'stopped'} in all nodes`);
    }
  },

  start(input) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterInputStatesController.start(input.id).url);
    return fetch('PUT', url)
      .then(
        (response) => {
          this._checkInputStateChangeResponse(input, response, 'START');
          this.list();
          return response;
        },
        (error) => {
          UserNotification.error(`Error starting input '${input.title}': ${error}`, `Input '${input.title}' could not be started`);
        },
      );
  },

  stop(input) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterInputStatesController.stop(input.id).url);
    return fetch('DELETE', url)
      .then(
        (response) => {
          this._checkInputStateChangeResponse(input, response, 'STOP');
          this.list();
          return response;
        },
        (error) => {
          UserNotification.error(`Error stopping input '${input.title}': ${error}`, `Input '${input.title}' could not be stopped`);
        },
      );
  },
});

export default InputStatesStore;
