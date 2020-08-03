// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';

type Token = {
  token_name: string,
  token: string,
  last_access: string,
};

type ChangePasswordRequest = {
  old_password: string,
  password: string,
};

const UsersStore = Reflux.createStore({
  create(request: any): Promise<string[]> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.create().url);
    const promise = fetch('POST', url, request);

    return promise;
  },

  loadUsers(): Promise<Immutable.List<User>> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.list().url);
    const promise = fetch('GET', url)
      .then(
        (response) => Immutable.List(response.users.map((user) => User.fromJSON(user))),
        (error) => {
          if (error.additional.status !== 404) {
            UserNotification.error(`Loading user list failed with status: ${error}`,
              'Could not load user list');
          }
        },
      );

    return promise;
  },

  load(username: string): Promise<User> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url);
    const promise = fetch('GET', url).then((loadedUser) => User.fromJSON(loadedUser));

    promise.catch((error) => {
      UserNotification.error(`Loading user failed with status: ${error}`,
        `Could not load user ${username}`);
    });

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

    return promise;
  },

  changePassword(username: string, request: ChangePasswordRequest): void {
    const url = qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },

  update(username: string, request: any): void {
    const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },

  createToken(username: string, tokenName: string): Promise<Token> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.create_token(encodeURIComponent(username),
      encodeURIComponent(tokenName)).url);
    const promise = fetch('POST', url);

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

    return promise;
  },

  loadTokens(username: string): Promise<Token[]> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.list_tokens(encodeURIComponent(username)).url);
    const promise = fetch('GET', url)
      .then(
        (response) => response.tokens,
        (error) => {
          UserNotification.error(`Loading tokens of user failed with status: ${error}`,
            `Could not load tokens of user ${username}`);
        },
      );

    return promise;
  },
});

export default UsersStore;
