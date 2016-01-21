/// <reference path="../../../declarations/jquery/jquery.d.ts" />

declare var $: any;
declare var jsRoutes: any;

const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');


export var LdapGroupsStore = {
    loadGroups(): JQueryPromise<string[]> {
        var promise = $.getJSON(jsRoutes.controllers.LdapController.apiGroups().url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading LDAP group list failed with status: " + errorThrown,
                    "Could not load LDAP group list");
            }
        });
        return promise;
    },
    loadMapping(): JQueryPromise<Object> {
        var promise = $.getJSON(jsRoutes.controllers.LdapController.apiLoadGroupMapping().url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading LDAP group mapping failed with status: " + errorThrown,
                    "Could not load LDAP group mapping");
            }
        });
        return promise;
    },
    saveMapping(mapping) {
        var promise = $.ajax({
            type: "POST",
            url: jsRoutes.controllers.LdapController.apiSaveGroupMapping().url,
            data: JSON.stringify(mapping),
            contentType: 'application/json',
        });

        promise.done(() => {
            UserNotification.success("LDAP group mapping successfully updated.");
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Updating LDAP group mapping failed with status: " + errorThrown,
                    "Could not update LDAP group mapping");
            }
        });
    }
};
