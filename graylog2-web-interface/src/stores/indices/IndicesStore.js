import Reflux from 'reflux';
import jQuery from 'jquery';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import IndicesActions from 'actions/indices/IndicesActions';

const IndicesStore = Reflux.createStore({
  listenables: [IndicesActions],
  indices: undefined,
  openIndices: undefined,
  closedIndices: undefined,

  init() {
    IndicesActions.list();
  },
  getInitialState() {
    return { indices: this.indices, closedIndices: this.closedIndices };
  },
  list() {
    const urlList = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.list().url);
    const promise = fetch('GET', urlList).then((response) => {
      this.indices = response.all.indices;
      this.closedIndices = response.closed.indices;
      this.trigger({ indices: this.indices, openIndices: this.openIndices, closedIndices: this.closedIndices });
      return { indices: this.indices, openIndices: this.openIndices, closedIndices: this.closedIndices };
    });

    IndicesActions.list.promise(promise);
  },
  listOpen() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.listOpen().url);
    const promise = fetch('GET', url).then((response) => {
      this.openIndices = response.indices;
      this.trigger({ indices: this.indices, openIndices: this.openIndices, closedIndices: this.closedIndices });
      return { indices: this.indices, openIndices: this.openIndices, closedIndices: this.closedIndices };
    });

    IndicesActions.listOpen.promise(promise);
  },
  close(indexName) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.close(indexName).url);
    const promise = fetch('POST', url);

    IndicesActions.close.promise(promise);
  },
  closeCompleted() {
    IndicesActions.list();
  },
  delete(indexName) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.delete(indexName).url);
    const promise = fetch('DELETE', url);

    IndicesActions.delete.promise(promise);
  },
  deleteCompleted() {
    IndicesActions.list();
  },
  reopen(indexName) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndicesApiController.reopen(indexName).url);
    const promise = fetch('POST', url);

    IndicesActions.reopen.promise(promise);
  },
  reopenCompleted() {
    IndicesActions.list();
  },
});

export default IndicesStore;
