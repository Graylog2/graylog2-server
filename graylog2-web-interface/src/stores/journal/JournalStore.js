import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const JournalStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/journal`,

  get(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl(nodeId)));
    promise.catch((error) => {
      UserNotification.error(`Getting journal information on node ${nodeId} failed: ${error}`, 'Could not get journal information');
    });

    return promise;
  },
});

export default JournalStore;
