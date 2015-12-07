import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import DeflectorActions from 'actions/indices/DeflectorActions';

const DeflectorStore = Reflux.createStore({
  listenables: [DeflectorActions],
  deflector: {
    config: undefined,
    info: undefined,
  },
  init() {
    DeflectorActions.list();
  },
  getInitialState() {
    return { deflector: this.deflector };
  },
  cycle() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DeflectorApiController.cycle().url);
    const promise = fetch('POST', url);

    DeflectorActions.cycle.promise(promise);
  },
  cycleCompleted() {
    DeflectorActions.config();
    DeflectorActions.list();
  },
  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DeflectorApiController.list().url);
    const promise = fetch('GET', url).then((info) => {
      this.deflector.info = info;
      this.deflector.config = info.config;
      this.trigger({deflector: this.deflector});
    });

    DeflectorActions.list.promise(promise);
  },
});

export default DeflectorStore;
