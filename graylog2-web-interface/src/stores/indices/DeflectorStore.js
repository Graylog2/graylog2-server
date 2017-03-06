import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const DeflectorActions = ActionsProvider.getActions('Deflector');

const DeflectorStore = Reflux.createStore({
  listenables: [DeflectorActions],
  deflector: {
    info: undefined,
  },
  getInitialState() {
    return { deflector: this.deflector };
  },
  cycle(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DeflectorApiController.cycle(indexSetId).url);
    const promise = fetch('POST', url);

    DeflectorActions.cycle.promise(promise);
  },
  list(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DeflectorApiController.list(indexSetId).url);
    const promise = fetch('GET', url).then((info) => {
      this.deflector.info = info;
      this.trigger({ deflector: this.deflector });
    });

    DeflectorActions.list.promise(promise);
  },
});

export default DeflectorStore;
