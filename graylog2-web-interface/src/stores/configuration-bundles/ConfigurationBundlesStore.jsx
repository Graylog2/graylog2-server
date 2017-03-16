import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch, { Builder, FetchError } from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');
const ConfigurationBundlesActions = ActionsProvider.getActions('ConfigurationBundles');

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');

const ConfigurationBundlesStore = Reflux.createStore({
  listenables: [ConfigurationBundlesActions],

  apply(bundleId) {
    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.BundlesApiController.apply(bundleId).url));

    ConfigurationBundlesActions.apply.promise(promise);
  },

  create(request) {
    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.BundlesApiController.create().url), request);

    ConfigurationBundlesActions.create.promise(promise);
  },

  createCompleted() {
    this.list();
  },

  delete(bundleId) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(ApiRoutes.BundlesApiController.delete(bundleId).url));

    ConfigurationBundlesActions.delete.promise(promise);
  },

  deleteCompleted() {
    this.list();
  },

  export(request) {
    const builder = new Builder('POST', URLUtils.qualifyUrl(ApiRoutes.BundlesApiController.export().url))
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
    const promise = fetch('GET', URLUtils.qualifyUrl(ApiRoutes.BundlesApiController.list().url))
      .then((result) => {
        this.trigger({ configurationBundles: result });

        return result;
      });

    ConfigurationBundlesActions.list.promise(promise);
  },
});

export default ConfigurationBundlesStore;
