// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import type { Store } from 'stores/StoreTypes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore } from 'views/logic/singleton';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import PaginationURL from 'util/PaginationURL';
import type {
  AuthenticationBackendCreate,
  AuthenticationBackendUpdate,
  ConnectionTestPayload,
  ConnectionTestResult,
  LoadActiveResponse,
  LoadResponse,
  LoginTestPayload,
  LoginTestResult,
  PaginatedAuthUsers,
  PaginatedBackends,
} from 'actions/authentication/AuthenticationActions';
import type { PaginatedResponseType } from 'stores/PaginationTypes';
import type { AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';
import ApiRoutes from 'routing/ApiRoutes';

import { userList as authUsers } from '../../../test/fixtures/userOverviews';

type PaginatedBackendsResponse = PaginatedResponseType & {
  context: {
    active_backend: ?AuthenticationBackendJSON,
  },
  backends: Array<AuthenticationBackendJSON>,
};

// type PaginatedUsersResponse = PaginatedResponseType & {
//   users: Array<UserOverviewJSON>,
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

    create(authBackend: AuthenticationBackendCreate): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.create().url);
      const promise = fetch('POST', url, authBackend);
      AuthenticationActions.create.promise(promise);

      return promise;
    },

    load(backendId: string): Promise<?LoadResponse> {
      const url = qualifyUrl(ApiRoutes.Authentication.load(encodeURIComponent(backendId)).url);
      const promise = fetch('GET', url).then((result) => (result ? {
        backend: AuthenticationBackend.fromJSON(result.backend),
      } : null));

      AuthenticationActions.load.promise(promise);

      return promise;
    },

    loadActive(): Promise<?LoadActiveResponse> {
      const url = qualifyUrl(ApiRoutes.Authentication.loadActive().url);
      const promise = fetch('GET', url).then((result) => (result ? {
        backend: result.backend ? AuthenticationBackend.fromJSON(result.backend) : null,
        context: { backendsTotal: result.context.backends_total },
      } : null));

      AuthenticationActions.loadActive.promise(promise);

      return promise;
    },

    update(backendId: ?$PropertyType<AuthenticationBackend, 'id'>, payload: AuthenticationBackendUpdate): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.update(backendId).url);
      const promise = fetch('PUT', url, payload);
      AuthenticationActions.update.promise(promise);

      return promise;
    },

    delete(backendId: ?$PropertyType<AuthenticationBackend, 'id'>): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.delete(backendId).url);
      const promise = fetch('DELETE', url);
      AuthenticationActions.delete.promise(promise);

      return promise;
    },

    testConnection(payload: ConnectionTestPayload): ConnectionTestResult {
      const url = qualifyUrl(ApiRoutes.Authentication.testConnection().url);
      const promise = fetch('POST', url, payload);
      AuthenticationActions.testConnection.promise(promise);

      return promise;
    },

    testLogin(payload: LoginTestPayload): LoginTestResult {
      const url = qualifyUrl(ApiRoutes.Authentication.testLogin().url);
      const promise = fetch('POST', url, payload);
      AuthenticationActions.testLogin.promise(promise);

      return promise;
    },

    enableUser(userId: string): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.enableUser(userId).url);
      const promise = fetch('POST', url);
      AuthenticationActions.enableUser.promise(promise);

      return promise;
    },

    disableUser(userId: string): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.disableUser(userId).url);
      const promise = fetch('POST', url);
      AuthenticationActions.disableUser.promise(promise);

      return promise;
    },

    setActiveBackend(backendId: ?$PropertyType<AuthenticationBackend, 'id'>): Promise<void> {
      const url = qualifyUrl(ApiRoutes.Authentication.updateConfiguration().url);
      const promise = fetch('POST', url, { active_backend: backendId });
      AuthenticationActions.setActiveBackend.promise(promise);

      return promise;
    },

    loadBackendsPaginated(page: number, perPage: number, query: string): Promise<?PaginatedBackends> {
      const url = PaginationURL(ApiRoutes.Authentication.servicesPaginated().url, page, perPage, query);
      const promise = fetch('GET', qualifyUrl(url))
        .then((response: PaginatedBackendsResponse) => ({
          context: {
            activeBackend: response.context.active_backend,
          },
          list: Immutable.List(response.backends.map((backend) => AuthenticationBackend.fromJSON(backend))),
          pagination: {
            count: response.count,
            total: response.total,
            page: response.page,
            perPage: response.per_page,
            query: response.query,
          },
        }));

      AuthenticationActions.loadBackendsPaginated.promise(promise);

      return promise;
    },

    loadUsersPaginated(page: number, perPage: number, query: string): Promise<?PaginatedAuthUsers> {
      // const url = PaginationURL(ApiRoutes.Authentication.loadUsersPaginated().url, page, perPage, query);

      // const promise = fetch('GET', qualifyUrl(url))
      // .then((response: PaginatedUsersResponse) => ({
      //   list: Immutable.List(response.users.map((user) => UserOverview.fromJSON(user))),
      //   pagination: {
      //     count: response.count,
      //     total: response.total,
      //     page: response.page,
      //     perPage: response.per_page,
      //     query: response.query,
      //   },
      // }));

      const promise = Promise.resolve({
        list: authUsers,
        pagination: {
          count: authUsers.size,
          total: authUsers.size,
          page: page || 1,
          perPage: perPage || 10,
          query: query || '',
        },
      });

      AuthenticationActions.loadUsersPaginated.promise(promise);

      return promise;
    },
  }),
);

export { AuthenticationActions, AuthenticationStore };

export default AuthenticationStore;
