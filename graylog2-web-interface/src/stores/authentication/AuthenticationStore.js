// @flow strict
import Reflux from 'reflux';

// import * as Immutable from 'immutable';
import type { Store } from 'stores/StoreTypes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { singletonStore } from 'views/logic/singleton';
import ActionsProvider from 'injection/ActionsProvider';
// import PaginationURL from 'util/PaginationURL';
import type { PaginatedServices } from 'actions/authentication/AuthenticationActions';
// import type { PaginatedResponseType } from 'stores/PaginationTypes';
// import AuthenticationService, { type AuthenticationServiceJson } from 'logic/authentication/AuthenticationService';
// import type { AuthenticationServiceJson } from 'logic/authentication/AuthenticationService';
// import ApiRoutes from 'routing/ApiRoutes';

import { services } from '../../../test/fixtures/authentication';

const TMP_SOURCE_URL = '/system/authentication/config';

const AuthenticationActions = ActionsProvider.getActions('Authentication');
// type PaginatedResponse = PaginatedResponseType & {
//   global_config: {
//     active_backend: string,
//   },
//   backends: Array<AuthenticationServiceJson>,
// };

const AuthenticationStore: Store<{ authenticators: any }> = singletonStore(
  'Authentication',
  () => Reflux.createStore({
    listenables: [AuthenticationActions],

    getInitialState() {
      return {
        authenticators: null,
      };
    },

    loadServicesPaginated(page: number, perPage: number, query: string): Promise<?PaginatedServices> {
    // const url = PaginationURL(ApiRoutes.authentication.servicesPaginated().url, page, perPage, query);
    // const promise = fetch('GET', qualifyUrl(url))
    //   .then((response: PaginatedResponse) => ({
    //     globalConfig: {
    //       activeBackend: response.global_config.active_backend,
    //     },
    //     list: Immutable.List(response.backends.map((backend) => AuthenticationService.fromJSON(backend))),
    //     pagination: {
    //       count: response.count,
    //       total: response.total,
    //       page: response.page,
    //       perPage: response.per_page,
    //       query: response.query,
    //     },
    //   }));

      const promise = Promise.resolve({
        globalConfig: {
          activeBackend: services.first().id,
        },
        list: services,
        pagination: {
          count: services.size,
          total: services.size,
          page: page || 1,
          perPage: perPage || 10,
          query: query || '',
        },
      });

      AuthenticationActions.loadServicesPaginated.promise(promise);

      return promise;
    },

    load() {
      const url = qualifyUrl(TMP_SOURCE_URL);
      const promise = fetch('GET', url)
        .then(
          (response) => {
            this.trigger({ authenticators: response });

            return response;
          },
          (error) => UserNotification.error(`Unable to load authentication configuration: ${error}`, 'Could not load authenticators'),
        );

      AuthenticationActions.load.promise(promise);
    },

    update(type, config) {
      const url = qualifyUrl(TMP_SOURCE_URL);

      if (type === 'providers') {
        const promise = fetch('PUT', url, config)
          .then(
            (response) => {
              this.trigger({ authenticators: response });
              UserNotification.success('Configuration updated successfully');

              return response;
            },
            (error) => UserNotification.error(`Unable to save authentication provider configuration: ${error}`, 'Could not save configuration'),
          );

        AuthenticationActions.update.promise(promise);
      }
    },
  }),
);

export default AuthenticationStore;
