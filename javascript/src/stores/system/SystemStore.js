import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const SystemStore = Reflux.createStore({
  system: undefined,
  init() {
    this.info().then((response) => {
      this.trigger({system: response});
      this.system = response;
    });
  },
  getInitialState() {
    return {system: this.system};
  },
  info() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.SystemApiController.info().url);

    return fetch('GET', url);
  },
});

export default SystemStore;
