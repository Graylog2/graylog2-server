import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import IndicesActions from 'actions/indices/IndicesActions';

const IndicesStore = Reflux.createStore({
  listenables: [IndicesActions],
  indices: undefined,

  init() {
    IndicesActions.list();
  },
  getInitialState() {
    return { indices: this.indices };
  },
  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      const indices = response.indices;
      this.indices = indices;
      this.trigger({indices: indices});
      return response.indices;
    });

    IndicesActions.list.promise(promise);
  },
  close(indexId) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.close(indexId).url);
    const promise = fetch('POST', url);

    IndicesActions.close.promise(promise);
  },
  delete() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.delete(indexId).url);
    const promise = fetch('DELETE', url);

    IndicesActions.delete.promise(promise);
  },
  closeCompleted() {
    IndicesActions.list();
  },
  deleteCompleted() {
    IndicesActions.list();
  },
});

export default IndicesStore;
