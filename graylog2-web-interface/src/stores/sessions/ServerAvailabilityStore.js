import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');

const ServerAvailabilityStore = Reflux.createStore({
  listenables: [ServerAvailabilityActions],
  server: { up: true },
  init() {
    this.ping();
  },
  getInitialState() {
    return { server: this.server };
  },
  ping() {
    return new Builder('GET', URLUtils.qualifyUrl(ApiRoutes.ClusterApiResource.node().url)).build().then(
      () => ServerAvailabilityActions.reportSuccess(),
      (error) => ServerAvailabilityActions.reportError(error)
    );
  },
  reportError(error) {
    if (this.server.up) {
      this.server = {up: false, error: error};
      this.trigger({server: this.server});
    }
  },
  reportSuccess() {
    if (!this.server.up) {
      this.server = {up: true};
      this.trigger({server: this.server});
    }
  },
});

export default ServerAvailabilityStore;
