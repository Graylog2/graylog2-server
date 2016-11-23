import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';
const IndexSetsActions = ActionsProvider.getActions('IndexSets');

const IndexSetsStore = Reflux.createStore({
  listenables: [IndexSetsActions],

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.list().url);
    const promise = fetch('GET', url);
    promise
      .then(
        response => this.trigger({ indexSetsCount: response.total, indexSets: response.index_sets }),
        error => {
          UserNotification.error(`Fetching index sets list failed: ${error.message}`,
            'Could not retrieve index sets.');
        });

    IndexSetsActions.list.promise(promise);
  },

  listPaginated(skip, limit) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.listPaginated(skip, limit).url);
    const promise = fetch('GET', url);
    promise
      .then(
        response => this.trigger({ indexSetsCount: response.total, indexSets: response.index_sets }),
        error => {
          UserNotification.error(`Fetching index sets list failed: ${error.message}`,
            'Could not retrieve index sets.');
        });

    IndexSetsActions.listPaginated.promise(promise);
  },

  get(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSetId).url);
    const promise = fetch('GET', url);
    promise.then(
      response => {
        this.trigger({ indexSet: response });
        return response;
      },
      error => {
        UserNotification.error(`Fetching index set '${indexSetId}' failed with status: ${error.message}`, 'Could not retrieve index set.');
      }
    );

    IndexSetsActions.get.promise(promise);
  },

  update(indexSet) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSet.id).url);
    const promise = fetch('PUT', url, indexSet);
    promise.then(
      response => {
        UserNotification.success(`Successfully updated index set '${indexSet.id}'`, 'Success');
        this.trigger({ indexSet: response });
        return response;
      },
      error => {
        UserNotification.error(`Updating index set '${indexSet.id}' failed with status: ${error.message}`, 'Could not update index set.');
      }
    );

    IndexSetsActions.update.promise(promise);
  },

  create(indexSet) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexSetsApiController.create().url);
    const promise = fetch('POST', url, indexSet);
    promise.then(
      response => {
        UserNotification.success(`Successfully created index set '${response.id}'`, 'Success');
        this.trigger({ indexSet: response });
        return response;
      },
      error => {
        UserNotification.error(`Creating index set '${indexSet.id}' failed with status: ${error.message}`, 'Could not create index set.');
      }
    );

    IndexSetsActions.create.promise(promise);
  },
});

export default IndexSetsStore;
