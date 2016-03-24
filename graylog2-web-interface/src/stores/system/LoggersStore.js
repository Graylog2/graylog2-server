import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const LoggersActions = ActionsProvider.getActions('Loggers');

const LoggersStore = Reflux.createStore({
  listenables: [LoggersActions],
  state: {
    availableLoglevels: [
      'fatal',
      'error',
      'warn',
      'info',
      'debug',
      'trace',
    ],
  },
  init() {
    this.loggers();
    this.subsystems();
  },
  getInitialState() {
    return this.state;
  },
  loggers() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterLoggersResource.loggers().url);
    const promise = fetch('GET', url).then((response) => {
      this.state.loggers = response;
      this.trigger(this.state);
      return response;
    });

    LoggersActions.loggers.promise(promise);
  },
  subsystems() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterLoggersResource.subsystems().url);
    const promise = fetch('GET', url).then((response) => {
      this.state.subsystems = response;
      this.trigger(this.state);
      return response;
    });

    LoggersActions.loggers.promise(promise);
  },
  setSubsystemLoggerLevel(nodeId, subsystem, level) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterLoggersResource.setSubsystemLoggerLevel(nodeId, subsystem, level).url);
    const promise = fetch('PUT', url);
    promise.then(() => {
      this.init();
    });

    LoggersActions.setSubsystemLoggerLevel.promise(promise);
  },
});

export default LoggersStore;
