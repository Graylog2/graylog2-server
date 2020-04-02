import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const SystemShutdownStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/shutdown`,

  shutdown(nodeId) {
    return fetch('POST', URLUtils.qualifyUrl(this.sourceUrl(nodeId)))
      .then(
        () => {
          this.trigger({});
          UserNotification.success(`Node '${nodeId}' will shutdown shortly`);
        },
        (error) => {
          UserNotification.error(`Shutting down node '${nodeId}' failed: ${error}`,
            `Could not send shutdown signal to node '${nodeId}'`);
        },
      );
  },
});

export default SystemShutdownStore;
