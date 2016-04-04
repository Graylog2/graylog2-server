import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const IndexerOverviewActions = ActionsProvider.getActions('IndexerOverview');

const IndexerOverviewStore = Reflux.createStore({
  listenables: [IndexerOverviewActions],
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerOverviewApiResource.list().url);
    const promise = fetch('GET', url);
    promise.then((response) => {
      this.trigger({ indexerOverview: response });
    });

    IndexerOverviewActions.list.promise(promise);

    return promise;
  },
});

export default IndexerOverviewStore;
