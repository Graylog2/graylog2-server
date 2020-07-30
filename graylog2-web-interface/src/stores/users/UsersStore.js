// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import UsersActions from 'actions/users/UsersActions';
import { singletonStore } from 'views/logic/singleton';
import PaginationURL from 'util/PaginationURL';
import UserOverview from 'logic/users/UserOverview';
import User from 'logic/users/User';

import type { PaginatedResponseType, PaginationType } from '../PaginationTypes';

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
};

type StartPage = {
  id: string,
  type: string,
};

export type UserJSON = {
  username: string,
  id: string,
  full_name: string,
  email: string,
  permissions: string[],
  timezone: string,
  preferences?: any,
  roles: string[],

  read_only: boolean,
  external: boolean,
  session_timeout_ms: number,

  startpage?: StartPage,

  session_active: boolean,
  client_address: string,
  last_activity: string,
};

export type Token = {
  token_name: string,
  token: string,
  last_access: string,
};

export type ChangePasswordRequest = {
  old_password: string,
  password: string,
};

type PaginatedResponse = PaginatedResponseType & {
  users: Array<UserJSON>,
  context: {
    admin_user: UserJSON,
  },
};

type UsersStoreState = {
  paginatedList: {
    adminUser: ?UserOverview,
    list: ?Immutable.List<UserOverview>,
    pagination: PaginationType,
  },
  list: ?Immutable.List<UserOverview>,
  loadedUser: ?User,
};

type UsersStoreType = Store<UsersStoreState>;

const UsersStore: UsersStoreType = singletonStore(
  'Users',
  () => Reflux.createStore({
    listenables: [UsersActions],
    paginatedList: {
      adminUser: undefined,
      list: undefined,
      pagination: DEFAULT_PAGINATION,
    },
    list: undefined,
    loadedUser: undefined,

    getInitialState(): UsersStoreState {
      return this._state();
    },

    create(request: any): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.create().url);
      const promise = fetch('POST', url, request);

      UsersActions.create.promise(promise);

      return promise;
    },

    loadUsers(): Promise<UserJSON[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.list().url);
      const promise = fetch('GET', url).then(
        (response) => {
          const { users } = response;
          this.list = Immutable.List(response.users.map((user) => UserOverview.fromJSON(user)));
          this._trigger();

          return users;
        },
        (error) => {
          if (error.additional.status !== 404) {
            UserNotification.error(`Loading user list failed with status: ${error}`,
              'Could not load user list');
          }
        },
      );

      UsersActions.loadUsers.promise(promise);

      return promise;
    },

    searchPaginated(page: number, perPage: number, query: string): Promise<UserJSON[]> {
      const url = PaginationURL(ApiRoutes.UsersApiController.paginated().url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then((response: PaginatedResponse) => {
          this.paginatedList = {
            adminUser: UserOverview.fromJSON(response.context.admin_user),
            list: Immutable.List(response.users.map((user) => UserOverview.fromJSON(user))),
            pagination: {
              count: response.count,
              total: response.total,
              page: response.page,
              perPage: response.per_page,
              query: response.query,
            },
          };

          this._trigger();

          return response.users;
        })
        .catch((errorThrown) => {
          UserNotification.error(`Loading user list failed with status: ${errorThrown}`,
            'Could not load user list');
        });

      UsersActions.searchPaginated.promise(promise);

      return promise;
    },

    load(username: string): Promise<UserJSON> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url);
      const promise = fetch('GET', url);

      promise.then((loadedUser) => {
        this.loadedUser = User.fromJSON(loadedUser);
        this._trigger();
      }).catch((error) => {
        UserNotification.error(`Loading user failed with status: ${error}`,
          `Could not load user ${username}`);
      });

      UsersActions.load.promise(promise);

      return promise;
    },

    deleteUser(username: string): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(username)).url);
      const promise = fetch('DELETE', url);

      promise.then(() => {
        UserNotification.success(`User "${username}" was deleted successfully`);
      }, (error) => {
        if (error.additional.status !== 404) {
          UserNotification.error(`Delete user failed with status: ${error}`,
            'Could not delete user');
        }
      });

      UsersActions.deleteUser.promise(promise);

      return promise;
    },

    changePassword(username: string, request: ChangePasswordRequest): void {
      const url = qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(username)).url);
      const promise = fetch('PUT', url, request);
      UsersActions.changePassword.promise(promise);

      return promise;
    },

    update(username: string, request: any): void {
      const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
      const promise = fetch('PUT', url, request);
      UsersActions.update.promise(promise);

      return promise;
    },

    createToken(username: string, tokenName: string): Promise<Token> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.create_token(encodeURIComponent(username),
        encodeURIComponent(tokenName)).url);
      const promise = fetch('POST', url);
      UsersActions.createToken.promise(promise);

      return promise;
    },

    deleteToken(username: string, tokenId: string, tokenName: string): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.delete_token(encodeURIComponent(username),
        encodeURIComponent(tokenId)).url, {});
      const promise = fetch('DELETE', url);

      promise.then(() => {
        UserNotification.success(`Token "${tokenName}" of user "${username}" was deleted successfully`);
      }, (error) => {
        if (error.additional.status !== 404) {
          UserNotification.error(`Delete token "${tokenName}" of user failed with status: ${error}`,
            'Could not delete token.');
        }
      });

      UsersActions.deleteToken.promise(promise);

      return promise;
    },

    loadTokens(username: string): Promise<Token[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.list_tokens(encodeURIComponent(username)).url);
      const promise = fetch('GET', url).then(
        (response) => response.tokens,
        (error) => {
          UserNotification.error(`Loading tokens of user failed with status: ${error}`,
            `Could not load tokens of user ${username}`);
        },
      );
      UsersActions.loadTokens.promise(promise);

      return promise;
    },

    _state(): UsersStoreState {
      return {
        list: this.list,
        paginatedList: this.paginatedList,
        loadedUser: this.loadedUser,
      };
    },

    _trigger() {
      this.trigger(this._state());
    },
  }),
);

export { UsersActions, UsersStore };
export default UsersStore;
