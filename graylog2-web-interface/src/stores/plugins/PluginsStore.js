import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const PluginsStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/plugins`,

  list(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl(nodeId)))
      .then(
        (response) => response.plugins,
        (error) => UserNotification.error(`Getting plugins on node "${nodeId}" failed: ${error}`, 'Could not get plugins'),
      );

    return promise;
  },
});

export default PluginsStore;
