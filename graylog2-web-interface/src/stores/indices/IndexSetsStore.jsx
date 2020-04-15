import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';

const IndexSetsActions = ActionsProvider.getActions('IndexSets');

const IndexSetsStore = Reflux.createStore({
  listenables: [IndexSetsActions],

  list(stats) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.list(stats).url);
    const promise = fetch('GET', url);
    promise
      .then(
        (response) => this.trigger({
          indexSetsCount: response.total,
          indexSets: response.index_sets,
          indexSetStats: response.stats,
        }),
        (error) => {
          UserNotification.error(`Fetching index sets list failed: ${error.message}`,
            'Could not retrieve index sets.');
        },
      );

    IndexSetsActions.list.promise(promise);
  },

  listPaginated(skip, limit, stats) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.listPaginated(skip, limit, stats).url);
    const promise = fetch('GET', url);
    promise
      .then(
        (response) => this.trigger({
          indexSetsCount: response.total,
          indexSets: response.index_sets,
          indexSetStats: response.stats,
        }),
        (error) => {
          UserNotification.error(`Fetching index sets list failed: ${this._errorMessage(error)}`,
            'Could not retrieve index sets.');
        },
      );

    IndexSetsActions.listPaginated.promise(promise);
  },

  get(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSetId).url);
    const promise = fetch('GET', url);
    promise.then(
      (response) => {
        this.trigger({ indexSet: response });
        return response;
      },
      (error) => {
        UserNotification.error(`Fetching index set '${indexSetId}' failed with status: ${this._errorMessage(error)}`, 'Could not retrieve index set.');
      },
    );

    IndexSetsActions.get.promise(promise);
  },

  update(indexSet) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSet.id).url);
    const promise = fetch('PUT', url, indexSet);
    promise.then(
      (response) => {
        UserNotification.success(`Successfully updated index set '${indexSet.title}'`, 'Success');
        this.trigger({ indexSet: response });
        return response;
      },
      (error) => {
        UserNotification.error(`Updating index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not update index set.');
      },
    );

    IndexSetsActions.update.promise(promise);
  },

  create(indexSet) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.create().url);
    const promise = fetch('POST', url, indexSet);
    promise.then(
      (response) => {
        UserNotification.success(`Successfully created index set '${indexSet.title}'`, 'Success');
        this.trigger({ indexSet: response });
        return response;
      },
      (error) => {
        UserNotification.error(`Creating index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not create index set.');
      },
    );

    IndexSetsActions.create.promise(promise);
  },

  delete(indexSet, deleteIndices) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.delete(indexSet.id, deleteIndices).url);
    const promise = fetch('DELETE', url);
    promise.then(
      () => {
        UserNotification.success(`Successfully deleted index set '${indexSet.title}'`, 'Success');
      },
      (error) => {
        UserNotification.error(`Deleting index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not delete index set.');
      },
    );

    IndexSetsActions.delete.promise(promise);
  },

  setDefault(indexSet) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.setDefault(indexSet.id).url);
    const promise = fetch('PUT', url);
    promise.then(
      () => {
        UserNotification.success(`Successfully set index set '${indexSet.title}' as default`, 'Success');
      },
      (error) => {
        UserNotification.error(`Setting index set '${indexSet.title}' as default failed with status: ${this._errorMessage(error)}`, 'Could not set default index set.');
      },
    );

    IndexSetsActions.setDefault.promise(promise);
  },

  stats() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.stats().url);
    const promise = fetch('GET', url);
    promise
      .then(
        (response) => this.trigger({
          globalIndexSetStats: {
            indices: response.indices,
            documents: response.documents,
            size: response.size,
          },
        }),
        (error) => {
          UserNotification.error(`Fetching global index stats failed: ${error.message}`,
            'Could not retrieve global index stats.');
        },
      );

    IndexSetsActions.stats.promise(promise);
  },

  _errorMessage(error) {
    try {
      return error.additional.body.message;
    } catch (e) {
      return error.message;
    }
  },
});

export default IndexSetsStore;
