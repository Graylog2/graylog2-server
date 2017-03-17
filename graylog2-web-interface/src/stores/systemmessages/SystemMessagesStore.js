import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { fetchPeriodically } from 'logic/rest/FetchProvider';

const SystemMessagesStore = Reflux.createStore({
  listenables: [],

  all(page) {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemMessagesApiController.all(page).url);

    return fetchPeriodically('GET', url);
  },
});

export default SystemMessagesStore;
