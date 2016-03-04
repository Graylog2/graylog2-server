import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const SystemMessagesStore = Reflux.createStore({
  listenables: [],

  all(page) {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemMessagesApiController.all(page).url);

    const promise = fetch('GET', url);
    return promise;
  },
});

export default SystemMessagesStore;
