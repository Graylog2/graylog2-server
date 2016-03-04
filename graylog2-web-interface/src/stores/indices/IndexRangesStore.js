import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import IndexRangesActions from 'actions/indices/IndexRangesActions';

const IndexRangesStore = Reflux.createStore({
  listenables: [IndexRangesActions],
  indexRanges: undefined,

  getInitialState() {
    return { indexRanges: this.indexRanges };
  },
  init() {
    IndexRangesActions.list();
  },
  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.IndexRangesApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      this.indexRanges = response.ranges;

      this.trigger(this.getInitialState());
    });

    IndexRangesActions.list.promise(promise);
  },
  recalculate() {
    const url = URLUtils.qualifyUrl(jsRoutes.IndexRangesApiController.rebuild().url);
    const promise = fetch ('POST', url);
    promise
      .then(UserNotification.success('Index ranges will be recalculated shortly'))
      .catch((error) => {
        UserNotification.error(`Could not create a job to start index ranges recalculation, reason: ${error}`,
          'Error starting index ranges recalculation');
      });

    IndexRangesActions.recalculate.promise(promise);
  },
  recalculateIndex(indexName) {
    const url = URLUtils.qualifyUrl(jsRoutes.IndexRangesApiController.rebuildSingle(indexName).url);
    const promise = fetch ('POST', url);
    promise
      .then(UserNotification.success(`Index ranges for ${indexName} will be recalculated shortly`))
      .catch((error) => {
        UserNotification.error(`Could not create a job to start index ranges recalculation for ${indexName}, reason: ${error}`,
          `Error starting index ranges recalculation for ${indexName}`);
      });

    IndexRangesActions.recalculateIndex.promise(promise);
  },
});

export default IndexRangesStore;
