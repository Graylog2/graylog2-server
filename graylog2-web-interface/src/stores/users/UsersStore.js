// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { singletonStore } from 'views/logic/singleton';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import PaginationURL from 'util/PaginationURL';
import UserOverview from 'logic/users/UserOverview';
import User, { type UserJSON } from 'logic/users/User';
import UsersActions, { type ChangePasswordRequest, type Token, type PaginatedUsers } from 'actions/users/UsersActions';
import type { PaginatedResponseType } from 'stores/PaginationTypes';

type PaginatedResponse = PaginatedResponseType & {
  users: Array<UserJSON>,
  context: {
    admin_user: UserJSON,
  },
};

const UsersStore: Store<{}> = singletonStore(
  'Users',
  () => Reflux.createStore({
    listenables: [UsersActions],

    create(request: any): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.create().url);
      const promise = fetch('POST', url, request);
      UsersActions.create.promise(promise);

      return promise;
    },

    load(username: string): Promise<User> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url);
      const promise = fetch('GET', url)
        .then(User.fromJSON)
        .catch((error) => {
          UserNotification.error(`Loading user failed with status: ${error}`,
            `Could not load user ${username}`);
        });

      UsersActions.load.promise(promise);

      return promise;
    },

    update(username: string, request: any): void {
      const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
      const promise = fetch('PUT', url, request);
      UsersActions.update.promise(promise);

      return promise;
    },

    delete(username: string): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(username)).url);
      const promise = fetch('DELETE', url).then(() => {
        UserNotification.success(`User "${username}" was deleted successfully`);
      }, (error) => {
        if (error.additional.status !== 404) {
          UserNotification.error(`Delete user failed with status: ${error}`,
            'Could not delete user');
        }
      });

      UsersActions.delete.promise(promise);

      return promise;
    },

    changePassword(username: string, request: ChangePasswordRequest): void {
      const url = qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(username)).url);
      const promise = fetch('PUT', url, request);
      UsersActions.changePassword.promise(promise);

      return promise;
    },

    createToken(username: string, tokenName: string): Promise<Token> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.create_token(encodeURIComponent(username),
        encodeURIComponent(tokenName)).url);
      const promise = fetch('POST', url);
      UsersActions.createToken.promise(promise);

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

    loadUsers(): Promise<Immutable.List<User>> {
      const url = qualifyUrl(ApiRoutes.UsersApiController.list().url);
      const promise = fetch('GET', url)
        .then(({ users }) => Immutable.List(users.map((user) => UserOverview.fromJSON(user))),
          (error) => {
            if (error.additional.status !== 404) {
              UserNotification.error(`Loading user list failed with status: ${error}`,
                'Could not load user list');
            }
          });

      UsersActions.loadUsers.promise(promise);

      return promise;
    },

    loadUsersPaginated(page: number, perPage: number, query: string): Promise<PaginatedUsers> {
      const url = PaginationURL(ApiRoutes.UsersApiController.paginated().url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then((response: PaginatedResponse) => ({
          adminUser: response.context.admin_user ? UserOverview.fromJSON(response.context.admin_user) : undefined,
          list: Immutable.List(response.users.map((user) => UserOverview.fromJSON(user))),
          pagination: {
            count: response.count,
            total: response.total,
            page: response.page,
            perPage: response.per_page,
            query: response.query,
          },
        }))
        .catch((errorThrown) => {
          UserNotification.error(`Loading user list failed with status: ${errorThrown}`,
            'Could not load user list');
        });

      UsersActions.loadUsersPaginated.promise(promise);

      return promise;
    },
  }),
);

export { UsersActions, UsersStore };
export default UsersStore;
