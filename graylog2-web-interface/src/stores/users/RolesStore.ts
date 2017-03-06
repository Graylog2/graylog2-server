/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');

// Need to use explicit require here to be able to access the User interface
import UsersStore = require("./UsersStore");

const ApiRoutes = require('routing/ApiRoutes');
const fetch = require('logic/rest/FetchProvider').default;

interface Role {
  name: string;
  description: string;
  permissions: string[];
}

interface RoleMembership {
  role: string;
  users: UsersStore.User[];
}

const RolesStore = {
  loadRoles(): Promise<string[]> {
    const promise = fetch('GET', URLUtils.qualifyUrl(ApiRoutes.RolesApiController.listRoles().url))
      .then(
        response => response.roles,
        error => {
          if (error.additional.status !== 404) {
            UserNotification.error("Loading role list failed with status: " + error,
              "Could not load role list");
          }
        }
      );

    return promise;
  },

  createRole(role: Role): Promise<Role> {
    const url = URLUtils.qualifyUrl(ApiRoutes.RolesApiController.createRole().url);
    const promise = fetch('POST', url, role);

    promise.then((newRole) => {
      UserNotification.success("Role \"" + newRole.name + "\" was created successfully");
    }, (error) => {
      UserNotification.error("Creating role \"" + role.name + "\" failed with status: " + error,
        "Could not create role");
    });

    return promise;
  },

  updateRole(rolename: string, role: Role): Promise<Role> {
    const promise = fetch('PUT', URLUtils.qualifyUrl(ApiRoutes.RolesApiController.updateRole(encodeURIComponent(rolename)).url), role);

    promise.then((newRole) => {
      UserNotification.success("Role \"" + newRole.name + "\" was updated successfully");
    }, (error) => {
      if (error.additional.status !== 404) {
        UserNotification.error("Updating role failed with status: " + error,
          "Could not update role");
      }
    });

    return promise;
  },

  deleteRole(rolename: string): Promise<string[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.RolesApiController.deleteRole(encodeURIComponent(rolename)).url);
    const promise = fetch('DELETE', url);

    promise.then(() => {
      UserNotification.success("Role \"" + rolename + "\" was deleted successfully");
    }, (error) => {
      if (error.additional.status !== 404) {
        UserNotification.error("Deleting role failed with status: " + error,
          "Could not delete role");
      }
    });
    return promise;
  },
  getMembers(rolename: string): Promise<RoleMembership[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.RolesApiController.loadMembers(encodeURIComponent(rolename)).url);
    const promise = fetch('GET', url);
    promise.catch((error) => {
      if (error.additional.status !== 404) {
        UserNotification.error("Could not load role's members with status: " + error,
          "Could not load role members");
      }
    });
    return promise;
  }
};

export = RolesStore;
