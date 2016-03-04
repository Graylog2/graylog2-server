import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const SystemMessagesStore = Reflux.createStore({
  listenables: [],

  all(page) {
    const url = URLUtils.qualifyUrl(jsRoutes.SystemMessagesApiController.all(page).url);

    const promise = fetch('GET', url);
    return promise;
  },
});

export default SystemMessagesStore;
