import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import { Builder } from 'logic/rest/FetchProvider';

import ServerAvailabilityActions from 'actions/sessions/ServerAvailabilityActions';

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
    return new Builder('GET', URLUtils.qualifyUrl(jsRoutes.ClusterApiResource.node().url)).build().then(
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
