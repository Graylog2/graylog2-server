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
        response => this.trigger({ indexSets: response.index_sets }),
        error => {
          UserNotification.error(`Fetching index sets list failed: ${error.message}`,
            'Could not retrieve index sets.');
        });

    IndexSetsActions.list.promise(promise);
  },
});

export default IndexSetsStore;
