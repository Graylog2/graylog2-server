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
    return new Builder('GET', URLUtils.qualifyUrl(ApiRoutes.ping().url))
      // Make sure to request JSON to avoid a redirect which breaks in Firefox (see https://github.com/Graylog2/graylog2-server/issues/3312)
      .setHeader('Accept', 'application/json')
      .setHeader('X-Graylog-No-Session-Extension', 'true')
      .build()
      .then(
        () => ServerAvailabilityActions.reportSuccess(),
        (error) => ServerAvailabilityActions.reportError(error),
      );
  },
  reportError(error) {
    if (this.server.up) {
      this.server = { up: false, error: error };
      this.trigger({ server: this.server });
    }
  },
  reportSuccess() {
    if (!this.server.up) {
      this.server = { up: true };
      this.trigger({ server: this.server });
    }
  },
});

export default ServerAvailabilityStore;
