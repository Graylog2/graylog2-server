/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
import ApiRoutes = require('routing/ApiRoutes');
const fetch = require('logic/rest/FetchProvider').default;

export interface StartPage {
  id: string;
  type: string;
}

export interface User {
  username: string;
  id: string;
  full_name: string;
  email: string;
  permissions: string[];
  timezone: string;
  preferences?: any;
  roles: string[];

  read_only: boolean;
  external: boolean;
  session_timeout_ms: number;

  startpage?: StartPage;
}

export interface ChangePasswordRequest {
  old_password: string;
  password: string;
}

export const UsersStore = {
  editUserFormUrl(username: string) {
    return URLUtils.qualifyUrl("/system/users/edit/" + username);
  },

  create(request: any): Promise<string[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.create().url);
    const promise = fetch('POST', url, request);
    return promise;
  },

  loadUsers(): Promise<User[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.list().url);
    const promise = fetch('GET', url)
      .then(
        response => response.users,
        (error) => {
          if (error.additional.status !== 404) {
            UserNotification.error("Loading user list failed with status: " + error,
              "Could not load user list");
          }
        });
    return promise;
  },

  load(username: string): Promise<User> {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url);
    const promise = fetch('GET', url);
    promise.catch((error) => {
      UserNotification.error("Loading user failed with status: " + error,
        "Could not load user " + username);
    });

    return promise;
  },

  deleteUser(username: string): Promise<string[]> {
    const  url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(username)).url);
    const  promise = fetch('DELETE', url);

    promise.then(() => {
      UserNotification.success("User \"" + username + "\" was deleted successfully");
    }, (error) => {
      if (error.additional.status !== 404) {
        UserNotification.error("Delete user failed with status: " + error,
          "Could not delete user");
      }
    });

    return promise;
  },

  updateRoles(username: string, roles: string[]): void {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, {roles: roles});

    return promise;
  },

  changePassword(username: string, request: ChangePasswordRequest): void {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },

  update(username: string, request: any): void {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(username)).url);
    const promise = fetch('PUT', url, request);

    return promise;
  },
};

module.exports = UsersStore;
