import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const SystemThreadDumpStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/threaddump`,

  get(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl(nodeId)))
      .then(
        (response) => {
          this.trigger({});
          return response.threaddump;
        },
        (error) => UserNotification.error(`Getting thread dump for node '${nodeId}' failed: ${error}`, 'Could not get thread dump')
      );

    return promise;
  },
});

export default SystemThreadDumpStore;
