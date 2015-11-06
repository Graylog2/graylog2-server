import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const IndexerClusterStore = Reflux.createStore({
  init() {
    this.health().then((health) => {
      this.trigger({health: health});
    });
    this.name().then((response) => {
      this.trigger({name: response.name});
    });
  },
  health() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndexerClusterApiController.health().url);
    return fetch('GET', url);
  },
  name() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndexerClusterApiController.name().url);
    return fetch('GET', url);
  },
});

export default IndexerClusterStore;
