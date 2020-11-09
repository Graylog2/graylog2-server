// @flow strict
import Reflux from 'reflux';

import HTTPHeaderAuthConfigActions from 'actions/authentication/HTTPHeaderAuthConfigActions';
import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import HTTPHeaderAuthConfig, { type HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';

const HTTPHeaderAuthConfigStore: Store<{}> = singletonStore(
  'HTTPHeaderAuthConfig',
  () => Reflux.createStore({
    listenables: [HTTPHeaderAuthConfigActions],

    load(): Promise<HTTPHeaderAuthConfig> {
      const url = qualifyUrl(ApiRoutes.HTTPHeaderAuthConfigController.load().url);
      const promise = fetch('GET', url).then(HTTPHeaderAuthConfig.fromJSON);

      HTTPHeaderAuthConfigActions.load.promise(promise);

      return promise;
    },

    update(payload: HTTPHeaderAuthConfigJSON): Promise<HTTPHeaderAuthConfig> {
      const url = qualifyUrl(ApiRoutes.HTTPHeaderAuthConfigController.update().url);
      const promise = fetch('PUT', url, payload).then(HTTPHeaderAuthConfig.fromJSON);

      HTTPHeaderAuthConfigActions.update.promise(promise);

      return promise;
    },
  }),
);

export { HTTPHeaderAuthConfigActions, HTTPHeaderAuthConfigStore };
