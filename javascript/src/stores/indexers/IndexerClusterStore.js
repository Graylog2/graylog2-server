import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import IndexerClusterActions from 'actions/indexers/IndexerClusterActions';

const IndexerClusterStore = Reflux.createStore({
  listenables: [IndexerClusterActions],
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
    const promise = fetch('GET', url);

    IndexerClusterActions.health.promise(promise);

    return promise;
  },
  name() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndexerClusterApiController.name().url);
    const promise = fetch('GET', url);

    IndexerClusterActions.name.promise(promise);

    return promise;
  },
});

export default IndexerClusterStore;
