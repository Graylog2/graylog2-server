import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch, { Builder, FetchError } from 'logic/rest/FetchProvider';
import jsRoutes from 'routing/jsRoutes';

import ConfigurationBundlesActions from 'actions/configuration-bundles/ConfigurationBundlesActions';
import SessionActions from 'actions/sessions/SessionActions';

import SessionStore from 'stores/sessions/SessionStore';

const ConfigurationBundlesStore = Reflux.createStore({
  listenables: [ConfigurationBundlesActions],

  apply(bundleId) {
    const promise = fetch('POST', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.apply(bundleId).url));

    ConfigurationBundlesActions.apply.promise(promise);
  },

  create(request) {
    const promise = fetch('POST', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.create().url), request);

    ConfigurationBundlesActions.create.promise(promise);
  },

  createCompleted() {
    this.list();
  },

  delete(bundleId) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.delete(bundleId).url));

    ConfigurationBundlesActions.delete.promise(promise);
  },

  deleteCompleted() {
    this.list();
  },

  export(request) {
    const builder = new Builder('POST', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.export().url))
      .authenticated()
      .build();
    const promise = builder
      .type('json')
      .accept('json')
      .send(request)
      .then((resp) => {
        if (resp.ok) {
          return resp.text;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => {
        if (error.status === 401) {
          SessionActions.logout(SessionStore.getSessionId());
        }

        throw new FetchError(error.statusText, error);
      });
    ConfigurationBundlesActions.export.promise(promise);
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.list().url))
      .then((result) => {
        this.trigger({configurationBundles: result});

        return result;
      });

    ConfigurationBundlesActions.list.promise(promise);
  },
});

export default ConfigurationBundlesStore;
