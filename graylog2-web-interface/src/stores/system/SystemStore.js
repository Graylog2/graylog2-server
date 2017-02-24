import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const SystemStore = Reflux.createStore({
  system: undefined,
  init() {
    this.info().then((response) => {
      this.trigger({ system: response });
      this.system = response;
    });
  },
  getInitialState() {
    return { system: this.system };
  },
  info() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.info().url);

    return fetch('GET', url);
  },
  jvm() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.jvm().url);

    return fetch('GET', url);
  },
});

export default SystemStore;
