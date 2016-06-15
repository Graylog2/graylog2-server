import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorsStore = Reflux.createStore({
  listenables: [DecoratorsActions],
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.get().url);
    const promise = fetch('GET', url);
    promise.then(response => {
      this.trigger({ decorators: response });
    });
    DecoratorsActions.list.promise(promise);

    return promise;
  },
  available() {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.available().url);
    const promise = fetch('GET', url);
    promise.then(response => {
      this.trigger({ types: response });
    });
    DecoratorsActions.available.promise(promise);

    return promise;
  },
  create(request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.create().url);
    const promise = fetch('POST', url, request);

    promise.then(() => DecoratorsActions.list());

    DecoratorsActions.create.promise(promise);

    return promise;
  },
});

export default DecoratorsStore;
