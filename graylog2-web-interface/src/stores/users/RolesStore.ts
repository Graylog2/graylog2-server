/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
import UsersStore = require('stores/users/UsersStore');
import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

export interface Role {
  name: string;
  description: string;
  permissions: string[];
}

export interface RoleMembership {
  role: string;
  users: UsersStore.User[];
}

export const RolesStore = {
  loadRoles(): Promise<string[]> {
    const promise = fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.RolesApiController.listRoles().url))
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
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.RolesApiController.createRole().url);
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
    const promise = fetch('PUT', URLUtils.qualifyUrl(jsRoutes.controllers.api.RolesApiController.updateRole(rolename).url), role);

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
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.RolesApiController.deleteRole(rolename).url);
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
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.RolesApiController.loadMembers(rolename).url);
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

export default RolesStore;
