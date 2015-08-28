/// <reference path="../../../declarations/jquery/jquery.d.ts" />

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");
import UsersStore = require("./UsersStore");

export interface Role {
    name: string;
    description: string;
    permissions: string[];
}

export interface RoleMembership {
    role: string;
    users: UsersStore.User[];
}

export var RolesStore = {
    loadRoles(): JQueryPromise<string[]> {
        var promise = $.getJSON(jsRoutes.controllers.api.RolesApiController.listRoles().url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading role list failed with status: " + errorThrown,
                    "Could not load role list");
            }
        });
        return promise;
    },
    createRole(role: Role): JQueryPromise<Role> {
        var url = jsRoutes.controllers.api.RolesApiController.createRole().url;
        var promise = $.ajax({
            type: "POST",
            url: url,
            data: JSON.stringify(role),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done((newRole) => {
            UserNotification.success("Role \"" + newRole.name + "\" was created successfully");
        });

        promise.fail((jqXHR) => {
            UserNotification.error("Creating role \"" + role.name + "\" failed with status: " + jqXHR.responseText,
                "Could not create role");
        });

        return promise;
    },

    updateRole(rolename: string, role: Role): JQueryPromise<Role> {
        var promise = $.ajax({
            type: 'PUT',
            url: jsRoutes.controllers.api.RolesApiController.updateRole(rolename).url,
            data: JSON.stringify(role),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done((newRole) => {
            UserNotification.success("Role \"" + newRole.name + "\" was updated successfully");
        });

        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Updating role failed with status: " + errorThrown,
                    "Could not update role");
            }
        });

        return promise;
    },

    deleteRole(rolename: string): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.RolesApiController.deleteRole(rolename).url;
        var promise = $.ajax({
            type: "DELETE",
            url: url
        });

        promise.done(() => {
            UserNotification.success("Role \"" + rolename + "\" was deleted successfully");
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Deleting role failed with status: " + errorThrown,
                    "Could not delete role");
            }
        });
        return promise;
    },
    getMembers(rolename: string): JQueryPromise<RoleMembership[]> {
        var promise = $.getJSON(jsRoutes.controllers.api.RolesApiController.loadMembers(rolename).url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Could not load role's members with status: " + errorThrown,
                    "Could not load role members");
            }
        });
        return promise;
    }
};
