import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const IndicesActions = ActionsProvider.getActions('Indices');

const IndicesStore = Reflux.createStore({
  listenables: [IndicesActions],
  indices: undefined,
  closedIndices: undefined,
  registrations: {},

  init() {
    IndicesActions.list();
  },
  getInitialState() {
    return { indices: this.indices, closedIndices: this.closedIndices };
  },
  list() {
    const urlList = URLUtils.qualifyUrl(ApiRoutes.IndicesApiController.list().url);
    const promise = fetch('GET', urlList).then((response) => {
      this.indices = response.all.indices;
      this.closedIndices = response.closed.indices;
      this.trigger({ indices: this.indices, closedIndices: this.closedIndices });
      return { indices: this.indices, closedIndices: this.closedIndices };
    });

    IndicesActions.list.promise(promise);
  },
  multiple() {
    const indexNames = Object.keys(this.registrations);
    if (indexNames.length <= 0) {
      return;
    }
    const urlList = URLUtils.qualifyUrl(ApiRoutes.IndicesApiController.multiple().url);
    const request = { indices: indexNames };
    const promise = fetch('POST', urlList, request).then((response) => {
      if (!this.indices) {
        this.indices = response;
      } else {
        Object.keys(response).forEach((indexName) => {
          this.indices[indexName] = response[indexName];
        });
      }
      this.trigger({ indices: this.indices, closedIndices: this.closedIndices });
      return { indices: this.indices, closedIndices: this.closedIndices };
    });

    IndicesActions.multiple.promise(promise);
  },
  close(indexName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndicesApiController.close(indexName).url);
    const promise = fetch('POST', url);

    IndicesActions.close.promise(promise);
  },
  closeCompleted() {
    IndicesActions.list();
  },
  delete(indexName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndicesApiController.delete(indexName).url);
    const promise = fetch('DELETE', url);

    IndicesActions.delete.promise(promise);
  },
  deleteCompleted() {
    IndicesActions.list();
  },
  reopen(indexName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndicesApiController.reopen(indexName).url);
    const promise = fetch('POST', url);

    IndicesActions.reopen.promise(promise);
  },
  reopenCompleted() {
    IndicesActions.list();
  },
  subscribe(indexName) {
    this.registrations[indexName] = this.registrations[indexName] ? this.registrations[indexName] + 1 : 1;
  },
  unsubscribe(indexName) {
    this.registrations[indexName] = this.registrations[indexName] > 0 ? this.registrations[indexName] - 1 : 0;
    if (this.registrations[indexName] === 0) {
      delete this.registrations[indexName];
    }
  },
});

export default IndicesStore;
