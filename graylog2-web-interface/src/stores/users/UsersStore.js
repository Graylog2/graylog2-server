import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const UsersStore = Reflux.createStore({
  create(request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.create().url);
    const promise = fetch('POST', url, request);
    return promise;
  },

  loadUsers() {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.list().url);
    const promise = fetch('GET', url)
      .then(
        response => response.users,
        (error) => {
          if (error.additional.status !== 404) {
            UserNotification.error(`Loading user list failed with status: ${error}`,
              'Could not load user list');
          }
        },
      );
    return promise;
  },

  load(username) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url);
    const promise = fetch('GET', url);
    promise.catch((error) => {
      UserNotification.error(`Loading user failed with status: ${error}`,
        `Could not load user ${username}`);
    });

    return promise;
  },

  deleteUser(username) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(username)).url);
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

  changePassword(username, request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },

  update(username, request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },

  createToken(username, tokenName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.create_token(encodeURIComponent(username),
      encodeURIComponent(tokenName)).url);
    const promise = fetch('POST', url);
    return promise;
  },

  deleteToken(username, token, tokenName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.delete_token(encodeURIComponent(username),
      encodeURIComponent(token)).url, {});
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

  loadTokens(username) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.list_tokens(encodeURIComponent(username)).url);
    const promise = fetch('GET', url)
      .then(
        response => response.tokens,
        (error) => {
          UserNotification.error(`Loading tokens of user failed with status: ${error}`,
            `Could not load tokens of user ${username}`);
        },
      );

    return promise;
  },
});

export default UsersStore;
