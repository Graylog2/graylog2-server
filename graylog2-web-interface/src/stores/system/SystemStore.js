import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import Promise from 'bluebird';

const SystemStore = Reflux.createStore({
  system: undefined,
  locales: undefined,
  init() {
    this.info().then((response) => {
      this.trigger({ system: response });
      this.system = response;
    });
    this.systemLocales().then((response) => {
      this.trigger({ locales: response });
      this.locales = response.locales;
    });
  },
  getInitialState() {
    return { system: this.system, locales: this.locales };
  },
  info() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.info().url);

    return fetch('GET', url);
  },
  jvm() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.jvm().url);

    return fetch('GET', url);
  },
  systemLocales() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.locales().url);

    return fetch('GET', url);
  },
  elasticsearchVersion() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterApiResource.elasticsearchStats().url);

    const promise = new Promise((resolve, reject) => {
      fetch('GET', url).then((response) => {
        const splitVersion = response.cluster_version.split('.');

        resolve({ major: splitVersion[0], minor: splitVersion[1], patch: splitVersion[2] });
      }).catch(reject);
    });

    return promise;
  },
});

export default SystemStore;
