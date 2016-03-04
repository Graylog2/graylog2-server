import Reflux from 'reflux';
import moment from 'moment';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const IndexerFailuresStore = Reflux.createStore({
  listenables: [],

  list(limit, offset) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.list(limit, offset).url);
    return fetch('GET', url);
  },

  count(since) {
    const momentSince = since.format ? since : moment(since);
    const isoSince = momentSince.format('YYYY-MM-DDTHH:mm:ss.SSS');
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.count(isoSince).url);

    return fetch('GET', url);
  },
});

export default IndexerFailuresStore;
