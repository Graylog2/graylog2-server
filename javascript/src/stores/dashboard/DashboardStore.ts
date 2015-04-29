/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare
var $: any;
declare
var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");

interface Dashboard {
    id: string;
    description: string;
    title: string;
}

var DashboardStore = {
    getWritableDashboardList(): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.listWritable().url;
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading your dashboard list failed with status: " + errorThrown,
                    "Could not load your dashboard list");
            }
        });
        return promise;
    },
    saveDashboard(dashboard: Dashboard, callback: () => void) {
        var url = jsRoutes.controllers.DashboardsController.update(dashboard.id).url;
        $.ajax({
            type: "POST",
            url: url,
            data: dashboard
        }).done(() => {
            callback();
            UserNotification.success("Dashboard successfully updated");
        }).fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving dashboard " + dashboard.id + " failed with status: " + errorThrown,
                "Could not save dashboard " + dashboard.title);
        });
    }
};
export = DashboardStore;
