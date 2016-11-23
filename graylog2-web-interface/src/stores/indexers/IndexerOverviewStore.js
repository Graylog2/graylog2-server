import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const IndexerOverviewActions = ActionsProvider.getActions('IndexerOverview');

const IndexerOverviewStore = Reflux.createStore({
  listenables: [IndexerOverviewActions],
  list(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerOverviewApiResource.list(indexSetId).url);
    const promise = fetch('GET', url);
    promise.then(
      (response) => {
        this.trigger({ indexerOverview: response, indexerOverviewError: undefined });
      },
      (error) => {
        if (error.additional && error.additional.status === 503) {
          const errorMessage = (error.additional.body && error.additional.body.message ?
            error.additional.body.message :
            'Elasticsearch is unavailable. Check your configuration and logs for more information.');
          this.trigger({ indexerOverviewError: errorMessage });
        }
      });

    IndexerOverviewActions.list.promise(promise);

    return promise;
  },
});

export default IndexerOverviewStore;
