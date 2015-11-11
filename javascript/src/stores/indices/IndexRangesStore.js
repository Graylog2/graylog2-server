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
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndexRangesApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      this.indexRanges = response.ranges;

      this.trigger(this.getInitialState());
    });

    IndexRangesActions.list.promise(promise);
  },
  recalculate() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.IndexRangesApiController.rebuild().url);
    const promise = fetch ('POST', url);

    IndexRangesActions.recalculate.promise(promise);
  },
});

export default IndexRangesStore;
