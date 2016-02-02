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
    const promise = new Builder('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterApiResource.node().url)).build().then(
      () => {
        this.server = {up: true};
        this.trigger({ server: this.server });
      },
      (error) => {
        this.server = { up: false, error: error };
        this.trigger({ server: this.server });
      },
    );

    return promise;
  },
  reportError(error) {
    this.server = { up: false, error: error };
    this.trigger({ server: this.server });
  },
  reportSucess() {
    this.server = { up: true };
    this.trigger({ server: this.server });
  },
});

export default ServerAvailabilityStore;
