/// <reference path="../../../declarations/jquery/jquery.d.ts" />

import UserNotification = require('util/UserNotification');
import URLUtils = require('util/URLUtils');
import jsRoutes = require('routing/jsRoutes');
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

const UsersStore = {
  editUserFormUrl(username: string) {
    return URLUtils.qualifyUrl("/system/users/edit/" + username);
  },

  loadUsers(): JQueryPromise<User[]> {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsersApiController.list().url);
    const promise = fetch('GET', url);
    promise.catch((jqXHR, textStatus, errorThrown) => {
      if (jqXHR.status !== 404) {
        UserNotification.error("Loading user list failed with status: " + errorThrown,
          "Could not load user list");
      }
    });
    return promise;
  },

  load(username: string): JQueryPromise<User> {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsersApiController.load(username).url);
    const promise = fetch('GET', url);
    promise.catch((jqXHR, textStatus, errorThrown) => {
      UserNotification.error("Loading user failed with status: " + errorThrown,
        "Could not load user " + username);
    });

    return promise;
  },

  deleteUser(username: string): JQueryPromise<string[]> {
    const  url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsersApiController.delete(username).url);
    const  promise = fetch('DELETE', url);

    promise.then(() => {
      UserNotification.success("User \"" + username + "\" was deleted successfully");
    }, (jqXHR, textStatus, errorThrown) => {
      if (jqXHR.status !== 404) {
        UserNotification.error("Delete user failed with status: " + errorThrown,
          "Could not delete user");
      }
    });

    return promise;
  }
};

export default UsersStore;
